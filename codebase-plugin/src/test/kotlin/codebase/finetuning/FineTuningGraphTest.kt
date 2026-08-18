package codebase.finetuning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * EPIC FT-PIPELINE US-3 — `FineTuningGraph` tests.
 *
 * Three nodes (propose → train → validate) chained by conditional edges
 * with a backtracking loop between PROPOSED and VALIDATED when the
 * validation score is below the threshold and the iteration budget is
 * not exhausted.
 *
 * The LLM is mocked via [FakeFineTuningLlm] and the pipeline via
 * [FakeFineTuner] (no network, no key). The prompt builder is a
 * deterministic stub.
 *
 * Baby-step US-3: graph topology + happy path + loop + failure modes.
 */
class FineTuningGraphTest {

    private val request = FineTuningRequest(
        baseModel = "gpt-oss:120b-cloud",
        dataset = listOf("docs/afnor/**/*.adoc"),
        outputModelName = "expert-cda",
        corpusRatio = 0.10,
    )

    private val success = FineTuningResult.Success(
        outputModelName = "expert-cda",
        ggufPath = "/tmp/expert-cda.gguf",
        iterations = 1,
        validationScore = 0.82,
    )

    private fun initialState(
        validationThreshold: Double = 0.7,
        maxIterations: Int = 3,
    ): FineTuningState = FineTuningState(
        request = request,
        validationThreshold = validationThreshold,
        maxIterations = maxIterations,
    )

    @Test
    fun `execute converges on the happy path when the first validation passes`() {
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.85),
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(initialState())

        assertThat(result.stage).isEqualTo(FineTuningStage.CONVERGED)
        assertThat(result.validationScore).isGreaterThanOrEqualTo(result.validationThreshold)
        assertThat(result.error).isNull()
        assertThat(result.trainResult).isInstanceOf(FineTuningResult.Success::class.java)
    }

    @Test
    fun `execute populates proposal from the propose node`() {
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "increase corpus ratio to 0.15", validateScore = 0.9),
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(initialState())

        assertThat(result.proposal).isEqualTo("increase corpus ratio to 0.15")
    }

    @Test
    fun `execute loops back to propose when validation is below threshold`() {
        val llm = FakeFineTuningLlm(
            proposeResponses = mutableListOf("ratio 0.10", "ratio 0.15", "ratio 0.20"),
            validateScores = mutableListOf(0.4, 0.5, 0.9),
        )
        val pipeline = FakeFineTuner(success)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = pipeline,
        )

        val result = graph.execute(initialState(validationThreshold = 0.7, maxIterations = 3))

        assertThat(result.stage).isEqualTo(FineTuningStage.CONVERGED)
        assertThat(result.validationScore).isEqualTo(0.9)
        assertThat(llm.proposeCallCount).isEqualTo(3)
        assertThat(llm.validateCallCount).isEqualTo(3)
        assertThat(pipeline.callCount).isEqualTo(3)
    }

    @Test
    fun `execute fails with MaxIterationsExhausted when the loop never converges`() {
        val llm = FakeFineTuningLlm(
            proposeResponses = mutableListOf("r0.10", "r0.15", "r0.20", "r0.25"),
            validateScores = mutableListOf(0.1, 0.2, 0.3, 0.4),
        )
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(initialState(validationThreshold = 0.7, maxIterations = 3))

        assertThat(result.stage).isEqualTo(FineTuningStage.FAILED)
        assertThat(result.error).contains("MaxIterationsExhausted")
    }

    @Test
    fun `execute fails immediately when train returns Failure`() {
        val failure = FineTuningResult.Failure("Ollama unavailable", request.dataset)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(failure),
        )

        val result = graph.execute(initialState())

        assertThat(result.stage).isEqualTo(FineTuningStage.FAILED)
        assertThat(result.error).contains("TrainReturnedFailure")
        assertThat(result.error).contains("Ollama unavailable")
    }

    @Test
    fun `execute fails when the propose LLM throws`() {
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeException = RuntimeException("LLM unavailable")),
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(initialState())

        assertThat(result.stage).isEqualTo(FineTuningStage.FAILED)
        assertThat(result.error).contains("ProposeFailed")
        assertThat(result.proposal).isEmpty()
    }

    @Test
    fun `execute fails when the train pipeline throws`() {
        val pipeline = object : FineTuningPipeline {
            override fun fineTune(request: FineTuningRequest): FineTuningResult =
                throw RuntimeException("training crashed")
        }
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = pipeline,
        )

        val result = graph.execute(initialState())

        assertThat(result.stage).isEqualTo(FineTuningStage.FAILED)
        assertThat(result.error).contains("TrainFailed")
    }

    @Test
    fun `execute fails when the validate LLM throws`() {
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(
                proposeResponse = "ratio 0.10",
                validateException = RuntimeException("scorer crashed"),
            ),
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(initialState())

        assertThat(result.stage).isEqualTo(FineTuningStage.FAILED)
        assertThat(result.error).contains("ValidateFailed")
    }

    @Test
    fun `execute calls the prompt builder with the running state for propose`() {
        val promptBuilder = FakePromptBuilder()
        val graph = FineTuningGraph(
            promptBuilder = promptBuilder,
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(success),
        )

        graph.execute(initialState())

        assertThat(promptBuilder.proposeCalls).hasSize(1)
        assertThat(promptBuilder.proposeCalls[0].stage).isEqualTo(FineTuningStage.INITIALIZED)
    }

    @Test
    fun `execute calls the prompt builder with the trained state for validate`() {
        val promptBuilder = FakePromptBuilder()
        val graph = FineTuningGraph(
            promptBuilder = promptBuilder,
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(success),
        )

        graph.execute(initialState())

        assertThat(promptBuilder.validateCalls).hasSize(1)
        assertThat(promptBuilder.validateCalls[0].stage).isEqualTo(FineTuningStage.TRAINED)
        assertThat(promptBuilder.validateCalls[0].trainResult).isNotNull
    }

    @Test
    fun `execute does not call validate when train returns Failure`() {
        val promptBuilder = FakePromptBuilder()
        val failure = FineTuningResult.Failure("boom", request.dataset)
        val graph = FineTuningGraph(
            promptBuilder = promptBuilder,
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(failure),
        )

        graph.execute(initialState())

        assertThat(promptBuilder.validateCalls).isEmpty()
    }

    @Test
    fun `execute forwards the LLM propose prompt to the provider`() {
        val llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = FakeFineTuner(success),
        )

        graph.execute(initialState())

        assertThat(llm.proposePrompts).hasSize(1)
        assertThat(llm.proposePrompts[0]).contains("expert-cda")
    }

    @Test
    fun `execute forwards the LLM validate prompt to the provider`() {
        val llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = FakeFineTuner(success),
        )

        graph.execute(initialState())

        assertThat(llm.validatePrompts).hasSize(1)
    }

    @Test
    fun `asMermaidDiagram returns a non-blank mermaid graph description`() {
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(success),
        )

        val mermaid = graph.asMermaidDiagram()

        assertThat(mermaid).isNotBlank()
    }

    @Test
    fun `execute preserves request and config through the pipeline`() {
        val config = FineTuningConfig(epochs = 5, learningRate = 1e-4)
        val state = FineTuningState(request = request, config = config)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = FakeFineTuningLlm(proposeResponse = "ratio 0.10", validateScore = 0.9),
            pipeline = FakeFineTuner(success),
        )

        val result = graph.execute(state)

        assertThat(result.request).isEqualTo(request)
        assertThat(result.config).isEqualTo(config)
    }

    @Test
    fun `execute parses corpus ratio adjustment from the proposal`() {
        val llm = FakeFineTuningLlm(proposeResponse = "increase corpus ratio to 0.15", validateScore = 0.9)
        val pipeline = FakeFineTuner(success)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = pipeline,
        )

        graph.execute(initialState())

        val captured = pipeline.lastRequest
        assertThat(captured).isNotNull
        assertThat(captured!!.corpusRatio).isEqualTo(0.15)
    }

    @Test
    fun `execute falls back to request corpus ratio when proposal has no numeric ratio`() {
        val llm = FakeFineTuningLlm(proposeResponse = "use more data", validateScore = 0.9)
        val pipeline = FakeFineTuner(success)
        val graph = FineTuningGraph(
            promptBuilder = FakePromptBuilder(),
            llm = llm,
            pipeline = pipeline,
        )

        graph.execute(initialState())

        assertThat(pipeline.lastRequest!!.corpusRatio).isEqualTo(0.10)
    }

    /** Deterministic prompt builder stub — records states for assertions. */
    private class FakePromptBuilder : FineTuningPromptBuilder {
        val proposeCalls = mutableListOf<FineTuningState>()
        val validateCalls = mutableListOf<FineTuningState>()

        override fun buildProposePrompt(state: FineTuningState): String {
            proposeCalls.add(state)
            return "Propose a corpus ratio adjustment for model='${state.request.outputModelName}'."
        }

        override fun buildValidatePrompt(state: FineTuningState): String {
            validateCalls.add(state)
            return "Evaluate the quality of the fine-tuned model '${state.request.outputModelName}'."
        }
    }

    /** Fake LLM — no network. Returns canned responses (or throws). */
    private class FakeFineTuningLlm(
        private val proposeResponse: String = "",
        private val validateScore: Double = 0.0,
        private val proposeResponses: MutableList<String>? = null,
        private val validateScores: MutableList<Double>? = null,
        private val proposeException: RuntimeException? = null,
        private val validateException: RuntimeException? = null,
    ) : FineTuningLlm {
        val proposePrompts = mutableListOf<String>()
        val validatePrompts = mutableListOf<String>()
        var proposeCallCount = 0
            private set
        var validateCallCount = 0
            private set

        override fun propose(prompt: String): String {
            proposeCallCount++
            proposePrompts.add(prompt)
            proposeException?.let { throw it }
            return when {
                proposeResponses != null && proposeResponses.isNotEmpty() -> proposeResponses.removeAt(0)
                else -> proposeResponse
            }
        }

        override fun validate(prompt: String): Double {
            validateCallCount++
            validatePrompts.add(prompt)
            validateException?.let { throw it }
            return when {
                validateScores != null && validateScores.isNotEmpty() -> validateScores.removeAt(0)
                else -> validateScore
            }
        }
    }
}