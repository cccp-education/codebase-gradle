package codebase.koog.pipeline

import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Generic koog pipeline skeleton shared by the N2 twins
 * `capsule.pipeline.CapsulePipelineGraph` and
 * `slider.pipeline.DeckPipelineGraph` (EPIC KOOG-PIPELINE-DEDUP, BS-1).
 *
 * Architecture ("koog orchestrates, langchain4j executes"):
 *  - [graph] is a koog [AIAgentGraphStrategy] declaring 3 nodes wired by
 *    conditional edges. It captures the *topology* (propose -> validate ->
 *    generate, with a `nodeFinish` short-circuit when validation fails) and is
 *    queryable via [asMermaidDiagram].
 *  - [execute] runs the pipeline sequentially with per-node try/catch. This
 *    keeps failure modes explicit and the unit tests free from koog's async
 *    runtime. A thrown propose failure maps through
 *    [PipelineDefinition.onProposeFailure]; a thrown generate failure maps
 *    through [PipelineDefinition.onGenerateFailure]. A failed propose state
 *    short-circuits immediately; an invalid validation short-circuits before
 *    generate.
 *
 * The domain-specific node logic (LLM calls, prompt building, validation,
 * failure labels) lives behind [PipelineDefinition] — this class carries no
 * domain knowledge and no Gradle/capsule/slider coupling.
 *
 * Non-perimeter (v1): no retry loop on validate, no Checkpoints, no
 * Self-Reflection, no RAG inside the graph.
 *
 * @param definition the domain port driving the three nodes.
 * @param graph      the koog strategy built from [definition] (see
 *                   [pipelineGraph]).
 */
class PipelineGraph<S : Any>(
    val definition: PipelineDefinition<S>,
    val graph: AIAgentGraphStrategy<S, S>,
) {

    private val log = LoggerFactory.getLogger(PipelineGraph::class.java)

    /**
     * Runs the pipeline sequentially from [initialState] to a final state.
     * Each node is wrapped in try/catch; a thrown failure surfaces as the
     * state returned by the definition's failure mapper.
     */
    fun execute(initialState: S): S {
        var state = try {
            definition.propose(initialState)
        } catch (e: Exception) {
            log.warn("[{}] propose-context failed: {}", definition.name, e.message)
            definition.onProposeFailure(initialState, e)
        }

        if (definition.isFailed(state)) return state

        state = definition.validate(state)
        if (!definition.isValid(state)) return state

        return try {
            definition.generate(state)
        } catch (e: Exception) {
            log.error("[{}] generate failed: {}", definition.name, e.message)
            definition.onGenerateFailure(state, e)
        }
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }
}

/**
 * Builds a [PipelineGraph] and its koog [AIAgentGraphStrategy] from a
 * [PipelineDefinition].
 *
 * `inline` + `reified S` because koog's `strategy` / `node` DSL builders
 * resolve their input/output [ai.koog.serialization.TypeToken] from reified
 * type parameters — a plain generic class could not call them. Call sites
 * (e.g. the adopting N2 graphs) pass a `PipelineDefinition<ConcreteState>` and
 * let `S` be inferred.
 */
inline fun <reified S : Any> pipelineGraph(definition: PipelineDefinition<S>): PipelineGraph<S> {
    val graph = strategy<S, S>(
        name = definition.name,
        toolSelectionStrategy = ToolSelectionStrategy.NONE,
    ) {
        val proposeNode by node<S, S> { state -> definition.propose(state) }
        val validateNode by node<S, S> { state -> definition.validate(state) }
        val generateNode by node<S, S> { state -> definition.generate(state) }

        edge(nodeStart forwardTo proposeNode onCondition { _ -> true } transformed { it })
        edge(proposeNode forwardTo validateNode onCondition { _ -> true } transformed { it })
        edge(validateNode forwardTo generateNode onCondition { definition.isValid(it) } transformed { it })
        edge(validateNode forwardTo nodeFinish onCondition { !definition.isValid(it) } transformed { it })
        edge(generateNode forwardTo nodeFinish onCondition { _ -> true } transformed { it })
    }

    return PipelineGraph(definition, graph)
}
