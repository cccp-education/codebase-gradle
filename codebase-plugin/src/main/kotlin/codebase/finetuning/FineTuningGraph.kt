package codebase.finetuning

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Koog-orchestrated iterative fine-tuning pipeline — the heart of EPIC
 * FT-PIPELINE US-3.
 *
 * Architecture ("koog orchestrates, langchain4j executes"):
 *  - The [graph] is a koog [AIAgentGraphStrategy] declaring 3 nodes wired by
 *    conditional edges. It captures the *topology* of the pipeline and is
 *    queryable via [asMermaidDiagram].
 *  - The [execute] method runs the pipeline sequentially with per-node
 *    try/catch, mirroring [codebase.koog.KoogAugmentedContextGraph.execute]
 *    and [slider.pipeline.DeckPipelineGraph.execute]. This keeps failure
 *    modes explicit and the unit tests free from koog's async runtime.
 *
 * Nodes:
 *  1. [proposeNode] — calls [FineTuningLlm.propose] with the prompt built
 *     from the running [FineTuningState]; stores the returned proposal and
 *     advances to [FineTuningStage.PROPOSED].
 *  2. [trainNode] — calls [FineTuningPipeline.fineTune] with the
 *     corpus-ratio-adjusted request; stores the [FineTuningResult] and
 *     advances to [FineTuningStage.TRAINED]. On [FineTuningResult.Failure]
 *     the pipeline transitions directly to [FineTuningStage.FAILED].
 *  3. [validateNode] — calls [FineTuningLlm.validate] with the prompt built
 *     from the trained state; stores the score and advances to
 *     [FineTuningStage.VALIDATED]. If `score >= threshold`, converges to
 *     [FineTuningStage.CONVERGED]; otherwise, loops back to [proposeNode]
 *     while `iteration < maxIterations`, else [FineTuningStage.FAILED].
 *
 * Conditional edges:
 *  - `train → validate onCondition { it.trainResult is Success }`
 *  - `train → finish    onCondition { it.trainResult is Failure }`
 *  - `validate → finish  onCondition { it.validationScore >= it.validationThreshold }`
 *  - `validate → propose onCondition { it.validationScore < it.validationThreshold && it.iteration < it.maxIterations }`
 *  - `validate → finish  onCondition { else }` (max iterations exhausted → FAILED)
 *
 * Non-périmètre (v1): no Checkpoints, no Self-Reflection — the loop is bounded
 * by [FineTuningState.maxIterations] (default `3`).
 *
 * @param promptBuilder builds the LLM prompts from the running state.
 * @param llm           the LLM provider (production: adapter on codebase's
 *                     `LlmBuildService`; tests: a fake).
 * @param pipeline      the fine-tuning pipeline (production: [OllamaFineTunerAdapter];
 *                     tests: [FakeFineTuner]).
 */
class FineTuningGraph(
    private val promptBuilder: FineTuningPromptBuilder,
    private val llm: FineTuningLlm,
    private val pipeline: FineTuningPipeline,
) {

    private val log = LoggerFactory.getLogger(FineTuningGraph::class.java)

    val graph: AIAgentGraphStrategy<FineTuningState, FineTuningState> =
        strategy<FineTuningState, FineTuningState>(
            name = "finetuning-pipeline",
            toolSelectionStrategy = ToolSelectionStrategy.NONE,
        ) {
            val propose by node<FineTuningState, FineTuningState> { state ->
                proposeNode(state)
            }
            val train by node<FineTuningState, FineTuningState> { state ->
                trainNode(state)
            }
            val validate by node<FineTuningState, FineTuningState> { state ->
                validateNode(state)
            }

            edge(nodeStart forwardTo propose onCondition { _ -> true } transformed { it })
            edge(propose forwardTo train onCondition { _ -> true } transformed { it })
            edge(train forwardTo validate onCondition { it.trainResult is FineTuningResult.Success } transformed { it })
            edge(train forwardTo nodeFinish onCondition { it.trainResult is FineTuningResult.Failure } transformed { it })
            edge(
                validate forwardTo nodeFinish
                    onCondition { it.validationScore >= it.validationThreshold }
                    transformed { it },
            )
            edge(
                validate forwardTo propose
                    onCondition { it.validationScore < it.validationThreshold && it.iteration < it.maxIterations }
                    transformed { it },
            )
            edge(
                validate forwardTo nodeFinish
                    onCondition { it.validationScore < it.validationThreshold && it.iteration >= it.maxIterations }
                    transformed { it },
            )
        }

    /**
     * Runs the iterative pipeline sequentially from [initialState] to a final
     * [FineTuningState]. Each node is wrapped in try/catch; failures surface
     * as [FineTuningStage.FAILED] with a non-null [FineTuningState.error].
     *
     * The loop bounded by [FineTuningState.maxIterations] re-enters [proposeNode]
     * when validation score is below threshold and the budget is not exhausted.
     */
    fun execute(initialState: FineTuningState): FineTuningState {
        var state = try {
            proposeNode(initialState)
        } catch (e: Exception) {
            log.warn("[FineTuningGraph] propose failed: {}", e.message)
            return initialState.copy(
                stage = FineTuningStage.FAILED,
                error = "ProposeFailed: ${e.message}",
            )
        }

        var iter = 0
        while (iter <= initialState.maxIterations) {
            state = try {
                trainNode(state)
            } catch (e: Exception) {
                log.warn("[FineTuningGraph] train failed: {}", e.message)
                return state.copy(
                    stage = FineTuningStage.FAILED,
                    error = "TrainFailed: ${e.message}",
                )
            }

            if (state.trainResult is FineTuningResult.Failure) {
                return state.copy(
                    stage = FineTuningStage.FAILED,
                    error = "TrainReturnedFailure: ${state.trainResult.reason}",
                )
            }

            state = try {
                validateNode(state)
            } catch (e: Exception) {
                log.warn("[FineTuningGraph] validate failed: {}", e.message)
                return state.copy(
                    stage = FineTuningStage.FAILED,
                    error = "ValidateFailed: ${e.message}",
                )
            }

            if (state.validationScore >= state.validationThreshold) {
                return state.copy(stage = FineTuningStage.CONVERGED)
            }

            iter += 1
            if (iter > initialState.maxIterations) {
                return state.copy(
                    stage = FineTuningStage.FAILED,
                    error = "MaxIterationsExhausted: $iter > ${initialState.maxIterations}",
                )
            }

            state = try {
                proposeNode(
                    state.copy(
                        iteration = iter,
                        proposal = "",
                        trainResult = null,
                        validationScore = 0.0,
                        stage = FineTuningStage.INITIALIZED,
                    ),
                )
            } catch (e: Exception) {
                log.warn("[FineTuningGraph] re-propose failed: {}", e.message)
                return state.copy(
                    stage = FineTuningStage.FAILED,
                    error = "ProposeFailed: ${e.message}",
                )
            }
        }

        return state.copy(
            stage = FineTuningStage.FAILED,
            error = "MaxIterationsExhausted",
        )
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }

    private fun proposeNode(state: FineTuningState): FineTuningState {
        val prompt = promptBuilder.buildProposePrompt(state)
        val proposal = llm.propose(prompt)
        require(proposal.isNotBlank()) { "LLM returned a blank proposal" }
        return state.copy(
            proposal = proposal,
            stage = FineTuningStage.PROPOSED,
        )
    }

    private fun trainNode(state: FineTuningState): FineTuningState {
        val adjustedRequest = state.request.copy(
            corpusRatio = adjustedCorpusRatio(state),
        )
        val result = pipeline.fineTune(adjustedRequest)
        return state.copy(
            trainResult = result,
            stage = FineTuningStage.TRAINED,
        )
    }

    private fun validateNode(state: FineTuningState): FineTuningState {
        val prompt = promptBuilder.buildValidatePrompt(state)
        val score = llm.validate(prompt)
        require(score in 0.0..1.0) { "LLM returned a validate score out of [0, 1]: $score" }
        return state.copy(
            validationScore = score,
            stage = FineTuningStage.VALIDATED,
        )
    }

    /**
     * Parses the LLM proposal to derive an adjusted corpus ratio. The proposal
     * is expected to contain a numeric ratio (e.g. "increase corpus ratio to
     * 0.15"); the parser extracts the first `Double` in `[0, 1]` found, or
     * falls back to the request's current ratio.
     */
    private fun adjustedCorpusRatio(state: FineTuningState): Double {
        val match = Regex("(\\d+\\.?\\d*)").findAll(state.proposal)
            .mapNotNull { it.value.toDoubleOrNull() }
            .firstOrNull { it in 0.0..1.0 }
        return match ?: state.request.corpusRatio
    }
}