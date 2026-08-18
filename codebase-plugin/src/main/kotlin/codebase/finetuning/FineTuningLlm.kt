package codebase.finetuning

/**
 * Port — the LLM provider consumed by [FineTuningGraph].
 *
 * Two operations match the two LLM-calling nodes of the iterative pipeline:
 *  - [propose]  → the `propose` node (returns a corpus-ratio adjustment string).
 *  - [validate] → the `validate` node (returns a quality score in `[0.0, 1.0]`).
 *
 * Synchronous contract — the domain `codebase.finetuning` stays Gradle-free,
 * coroutine-free, and unit-testable with a plain fake. The adapter mapping
 * `suspend` → blocking (production on codebase's `LlmBuildService`) lives
 * outside the domain.
 *
 * EPIC FT-PIPELINE US-3 (pattern `slider.pipeline.DeckLlm`).
 */
interface FineTuningLlm {

    /**
     * Calls the LLM with the propose prompt; returns the proposal string
     * (e.g. a corpus-ratio tweak, an adjusted hyperparameter).
     */
    fun propose(prompt: String): String

    /**
     * Calls the LLM with the validate prompt; returns a quality score
     * in `[0.0, 1.0]` for the fine-tuned model.
     */
    fun validate(prompt: String): Double
}