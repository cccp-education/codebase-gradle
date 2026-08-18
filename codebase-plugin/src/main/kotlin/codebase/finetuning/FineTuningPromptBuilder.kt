package codebase.finetuning

/**
 * Port — builds the LLM prompts for the iterative fine-tuning pipeline nodes.
 *
 * Pure — no LLM, no I/O. Kept as a port so [FineTuningGraph] can be unit-tested
 * with a deterministic stub, without depending on any adapter or Gradle.
 *
 * EPIC FT-PIPELINE US-3 (pattern `slider.pipeline.DeckPromptBuilder`).
 */
interface FineTuningPromptBuilder {

    /**
     * Prompt for the `propose` node — asks the LLM to suggest a corpus-ratio
     * adjustment (or other hyperparameter tweak) from the running [state].
     */
    fun buildProposePrompt(state: FineTuningState): String

    /**
     * Prompt for the `validate` node — asks the LLM (or a scoring model) to
     * evaluate the quality of the fine-tuned model described by [state].
     * Returns the prompt fed to the scorer.
     */
    fun buildValidatePrompt(state: FineTuningState): String
}