package codebase.koog.pipeline

/**
 * Domain port describing a propose -> validate -> generate pipeline to
 * [PipelineGraph].
 *
 * EPIC KOOG-PIPELINE-DEDUP (BS-1): the N2 twins
 * `capsule.pipeline.CapsulePipelineGraph` and
 * `slider.pipeline.DeckPipelineGraph` share the same koog topology, the same
 * sequential fail-fast [PipelineGraph.execute], and the same
 * [PipelineGraph.asMermaidDiagram]. Only the state type, the node logic and
 * the failure labels differ. Those differences live here; the orchestration
 * lives in the shared N1 skeleton.
 *
 * The three node functions are pure state transformations:
 *  - [propose] calls the LLM to obtain the context plan and stores it;
 *  - [validate] delegates to the pure validator and sets the validity flag;
 *  - [generate] calls the LLM to obtain the final artefact.
 *
 * A node may fail either by throwing (mapped by [onProposeFailure] /
 * [onGenerateFailure]) or by returning a state for which [isFailed] is true.
 * When [validate] returns an invalid state ([isValid] false), the pipeline
 * short-circuits before [generate].
 *
 * @param S the pipeline state type (e.g. `CapsuleState`, `DeckState`).
 */
interface PipelineDefinition<S> {

    /**
     * Strategy name exposed by the built koog graph (e.g.
     * `"capsule-pipeline"`, `"deck-pipeline"`).
     */
    val name: String

    /** Runs the propose-context node against [state]. */
    fun propose(state: S): S

    /** Runs the pure validate-context node against [state]. */
    fun validate(state: S): S

    /** Runs the generate-artefact node against [state]. */
    fun generate(state: S): S

    /** `true` when [state] passed validation and may reach [generate]. */
    fun isValid(state: S): Boolean

    /** `true` when [state] is in a terminal failed state. */
    fun isFailed(state: S): Boolean

    /** Maps a [propose] failure (thrown [cause]) onto a failed [state]. */
    fun onProposeFailure(state: S, cause: Throwable): S

    /** Maps a [generate] failure (thrown [cause]) onto a failed [state]. */
    fun onGenerateFailure(state: S, cause: Throwable): S
}
