package codebase.finetuning

/**
 * Stages of the iterative fine-tuning pipeline — sequential progression
 * from initialization to convergence (or failure), with backtracking loops
 * between [PROPOSED] and [VALIDATED] when the validation score is below
 * the threshold and the iteration budget is not exhausted.
 *
 * Pure enum — no Gradle, no LLM, no I/O. Drives the conditional edges of
 * [FineTuningGraph] and the invariants of [FineTuningState].
 *
 * EPIC FT-PIPELINE US-3 (pattern `slider.pipeline.DeckStage`).
 */
enum class FineTuningStage {
    /** Fresh state — request + config set; no LLM call yet. */
    INITIALIZED,

    /** LLM has proposed a corpus-ratio adjustment via the `propose` node. */
    PROPOSED,

    /** Pipeline.fineTune() has been called via the `train` node. */
    TRAINED,

    /** Validation score has been evaluated via the `validate` node. */
    VALIDATED,

    /** Convergence reached — validationScore >= validationThreshold. */
    CONVERGED,

    /** Pipeline failed — max iterations exhausted or train returned Failure. */
    FAILED,
}