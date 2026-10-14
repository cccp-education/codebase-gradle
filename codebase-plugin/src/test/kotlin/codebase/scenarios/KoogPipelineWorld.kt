package codebase.scenarios

import codebase.koog.pipeline.PipelineDefinition
import codebase.koog.pipeline.pipelineGraph

/**
 * Shared world for `@koog-pipeline` scenarios (EPIC KOOG-PIPELINE-DEDUP,
 * BS-1 — generic N1 koog pipeline skeleton).
 *
 * Drives the real `codebase.koog.pipeline.pipelineGraph` + `PipelineGraph`
 * with an in-memory [KoogPipelineDefinition] backed by [SampleState] — no
 * network, no LLM, no Gradle task. The fixtures mirror the shape of the two
 * N2 twins (`CapsuleState`/`DeckState`): a stage enum, a validity flag, a
 * validation error and an error field.
 *
 * PicoContainer-scoped, one fresh instance per scenario.
 */
class KoogPipelineWorld {

    var proposeThrows: String? = null
    var validateError: String? = null

    var definition: KoogPipelineDefinition? = null
    var result: SampleState? = null
    var mermaidDiagram: String? = null

    fun execute() {
        val definition = buildDefinition()
        this.definition = definition
        result = pipelineGraph(definition).execute(SampleState(subject = "kotlin-coroutines"))
    }

    fun requestMermaidDiagram() {
        val definition = buildDefinition()
        this.definition = definition
        mermaidDiagram = pipelineGraph(definition).asMermaidDiagram()
    }

    private fun buildDefinition(): KoogPipelineDefinition =
        KoogPipelineDefinition(proposeThrows = proposeThrows, validateError = validateError)
}

enum class SampleStage {
    INITIALIZED,
    CONTEXT_PROPOSED,
    CONTEXT_VALIDATED,
    CONTENT_GENERATED,
    FAILED,
}

data class SampleState(
    val subject: String = "",
    val stage: SampleStage = SampleStage.INITIALIZED,
    val planJson: String = "",
    val output: String = "",
    val contextValid: Boolean = false,
    val validationError: String? = null,
    val error: String? = null,
)

/**
 * Test-only [PipelineDefinition] mirroring the N2 twins' node logic: propose
 * (optionally throwing), pure validate (optionally rejecting), generate.
 */
class KoogPipelineDefinition(
    private val proposeThrows: String? = null,
    private val validateError: String? = null,
) : PipelineDefinition<SampleState> {

    override val name = "sample-pipeline"

    val calls = mutableListOf<String>()

    override fun propose(state: SampleState): SampleState {
        calls.add("propose")
        proposeThrows?.let { throw RuntimeException(it) }
        return state.copy(planJson = "valid-plan", stage = SampleStage.CONTEXT_PROPOSED)
    }

    override fun validate(state: SampleState): SampleState {
        calls.add("validate")
        return validateError?.let {
            state.copy(contextValid = false, validationError = it, error = it, stage = SampleStage.FAILED)
        } ?: state.copy(contextValid = true, stage = SampleStage.CONTEXT_VALIDATED)
    }

    override fun generate(state: SampleState): SampleState {
        calls.add("generate")
        return state.copy(output = "generated", stage = SampleStage.CONTENT_GENERATED)
    }

    override fun isValid(state: SampleState): Boolean = state.contextValid

    override fun isFailed(state: SampleState): Boolean = state.stage == SampleStage.FAILED

    override fun onProposeFailure(state: SampleState, cause: Throwable): SampleState =
        state.copy(stage = SampleStage.FAILED, error = "ProposeFailed: ${cause.message}")

    override fun onGenerateFailure(state: SampleState, cause: Throwable): SampleState =
        state.copy(stage = SampleStage.FAILED, error = "GenerateFailed: ${cause.message}")
}
