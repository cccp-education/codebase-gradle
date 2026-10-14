package codebase.koog.pipeline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PipelineGraph] — the generic koog pipeline skeleton
 * extracted from the verbatim twins `capsule.pipeline.CapsulePipelineGraph`
 * and `slider.pipeline.DeckPipelineGraph` (EPIC KOOG-PIPELINE-DEDUP, BS-1).
 *
 * The skeleton owns the orchestration only: the propose -> validate ->
 * generate topology ([pipelineGraph]), the sequential fail-fast [PipelineGraph.execute]
 * with per-node try/catch, and [PipelineGraph.asMermaidDiagram]. The
 * domain-specific node logic lives behind the [PipelineDefinition] port, which
 * these tests drive with [SampleState]/[RecordingDefinition] fixtures (no
 * network, no LLM, no Gradle).
 *
 * Baby-step TDD: call order, short-circuit on failed/invalid, failure
 * mapping, state preservation, mermaid topology.
 */
class PipelineGraphTest {

    enum class Stage {
        INITIALIZED,
        CONTEXT_PROPOSED,
        CONTEXT_VALIDATED,
        CONTENT_GENERATED,
        FAILED,
    }

    data class SampleState(
        val subject: String = "",
        val stage: Stage = Stage.INITIALIZED,
        val planJson: String = "",
        val output: String = "",
        val contextValid: Boolean = false,
        val validationError: String? = null,
        val error: String? = null,
    )

    private class RecordingDefinition(
        private val proposeResult: (SampleState) -> SampleState = {
            it.copy(planJson = "valid-plan", stage = Stage.CONTEXT_PROPOSED)
        },
        private val validateResult: (SampleState) -> SampleState = {
            it.copy(contextValid = true, stage = Stage.CONTEXT_VALIDATED)
        },
        private val generateResult: (SampleState) -> SampleState = {
            it.copy(output = "generated", stage = Stage.CONTENT_GENERATED)
        },
    ) : PipelineDefinition<SampleState> {

        override val name = "sample-pipeline"

        val calls = mutableListOf<String>()

        override fun propose(state: SampleState): SampleState {
            calls.add("propose")
            return proposeResult(state)
        }

        override fun validate(state: SampleState): SampleState {
            calls.add("validate")
            return validateResult(state)
        }

        override fun generate(state: SampleState): SampleState {
            calls.add("generate")
            return generateResult(state)
        }

        override fun isValid(state: SampleState): Boolean = state.contextValid

        override fun isFailed(state: SampleState): Boolean = state.stage == Stage.FAILED

        override fun onProposeFailure(state: SampleState, cause: Throwable): SampleState =
            state.copy(stage = Stage.FAILED, error = "ProposeFailed: ${cause.message}")

        override fun onGenerateFailure(state: SampleState, cause: Throwable): SampleState =
            state.copy(stage = Stage.FAILED, error = "GenerateFailed: ${cause.message}")
    }

    @Test
    fun `execute runs propose validate generate in order on the happy path`() {
        val definition = RecordingDefinition()
        val graph = pipelineGraph(definition)

        val result = graph.execute(SampleState())

        assertEquals(listOf("propose", "validate", "generate"), definition.calls)
        assertEquals(Stage.CONTENT_GENERATED, result.stage)
        assertEquals("generated", result.output)
        assertTrue(result.contextValid)
        assertNull(result.error)
    }

    @Test
    fun `execute stops after propose when the propose node throws and maps the failure`() {
        val definition = RecordingDefinition(proposeResult = { throw RuntimeException("boom") })
        val graph = pipelineGraph(definition)

        val result = graph.execute(SampleState())

        assertEquals(listOf("propose"), definition.calls)
        assertEquals(Stage.FAILED, result.stage)
        assertTrue(result.error.orEmpty().contains("ProposeFailed: boom"))
    }

    @Test
    fun `execute short-circuits when the propose node returns a failed state`() {
        val definition = RecordingDefinition(
            proposeResult = { it.copy(stage = Stage.FAILED, error = "already failed") },
        )
        val graph = pipelineGraph(definition)

        val result = graph.execute(SampleState())

        assertEquals(listOf("propose"), definition.calls)
        assertEquals(Stage.FAILED, result.stage)
        assertEquals("already failed", result.error)
    }

    @Test
    fun `execute skips the generate node when validation is invalid`() {
        val definition = RecordingDefinition(
            validateResult = {
                it.copy(contextValid = false, validationError = "bad plan", stage = Stage.FAILED)
            },
        )
        val graph = pipelineGraph(definition)

        val result = graph.execute(SampleState())

        assertEquals(listOf("propose", "validate"), definition.calls)
        assertFalse(result.contextValid)
        assertEquals("bad plan", result.validationError)
        assertEquals(Stage.FAILED, result.stage)
    }

    @Test
    fun `execute stops when the generate node throws and maps the failure`() {
        val definition = RecordingDefinition(generateResult = { throw IllegalStateException("timeout") })
        val graph = pipelineGraph(definition)

        val result = graph.execute(SampleState())

        assertEquals(listOf("propose", "validate", "generate"), definition.calls)
        assertEquals(Stage.FAILED, result.stage)
        assertTrue(result.contextValid)
        assertTrue(result.error.orEmpty().contains("GenerateFailed: timeout"))
    }

    @Test
    fun `execute preserves the untouched initial state fields through the pipeline`() {
        val graph = pipelineGraph(RecordingDefinition())

        val result = graph.execute(SampleState(subject = "kotlin-coroutines"))

        assertEquals("kotlin-coroutines", result.subject)
    }

    @Test
    fun `execute forwards the propose output state to the validate node`() {
        val observed = mutableListOf<SampleState>()
        val definition = RecordingDefinition(
            validateResult = { state -> observed.add(state); state.copy(contextValid = true, stage = Stage.CONTEXT_VALIDATED) },
        )
        val graph = pipelineGraph(definition)

        graph.execute(SampleState())

        assertEquals(1, observed.size)
        assertEquals(Stage.CONTEXT_PROPOSED, observed[0].stage)
        assertEquals("valid-plan", observed[0].planJson)
    }

    @Test
    fun `execute forwards the validated state to the generate node`() {
        val observed = mutableListOf<SampleState>()
        val definition = RecordingDefinition(
            generateResult = { state -> observed.add(state); state.copy(output = "generated", stage = Stage.CONTENT_GENERATED) },
        )
        val graph = pipelineGraph(definition)

        graph.execute(SampleState())

        assertEquals(1, observed.size)
        assertTrue(observed[0].contextValid)
        assertEquals(Stage.CONTEXT_VALIDATED, observed[0].stage)
    }

    @Test
    fun `asMermaidDiagram returns a non-blank description`() {
        val graph = pipelineGraph(RecordingDefinition())

        val mermaid = graph.asMermaidDiagram()

        assertTrue(mermaid.isNotBlank())
    }

    @Test
    fun `graph exposes the strategy named after the definition`() {
        val graph = pipelineGraph(RecordingDefinition())

        assertEquals("sample-pipeline", graph.graph.name)
    }
}
