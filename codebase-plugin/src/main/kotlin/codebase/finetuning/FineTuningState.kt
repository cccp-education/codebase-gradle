package codebase.finetuning

/**
 * Immutable state of the iterative fine-tuning pipeline, flowing through the
 * nodes of [FineTuningGraph]. Each node produces a new [FineTuningState] via
 * [copy], following the koog state pattern (data class + immutable progression).
 *
 * Pure value object — no Gradle, no LLM, no koog, no I/O. Fields model the
 * full lifecycle from initial request to the converged model (or failure),
 * including backtracking loops between PROPOSED and VALIDATED.
 *
 * Invariants — cross-field consistency validated in [init]:
 * - [proposal] non-blank requires stage >= PROPOSED.
 * - [trainResult] non-null requires stage >= TRAINED.
 * - [stage] VALIDATED requires [validationScore] in `[0.0, 1.0]` and [trainResult] non-null.
 * - [stage] CONVERGED requires [validationScore] >= [validationThreshold] and [trainResult] Success.
 * - [stage] FAILED requires a non-blank [error].
 * - [stage] FAILED allows [error] non-null only when stage == FAILED (else null).
 * - [iteration] in `0..maxIterations`.
 *
 * EPIC FT-PIPELINE US-3 (pattern `slider.pipeline.DeckState`).
 *
 * @param request             the immutable fine-tuning request.
 * @param config              hyperparameters (epochs, learningRate, …).
 * @param proposal            LLM-proposed corpus-ratio adjustment from the `propose` node;
 *                            blank until that node runs.
 * @param trainResult         outcome of `FineTuningPipeline.fineTune()`; null until `train` runs.
 * @param validationScore     quality score of the trained model — `0.0` until `validate` runs.
 * @param validationThreshold convergence threshold (default `0.7`, configurable). The pipeline
 *                            loops while `validationScore < validationThreshold` and the
 *                            iteration budget is not exhausted.
 * @param iteration           current iteration counter — `0` at start, incremented after each loop.
 * @param maxIterations      max iterations before FAILED (default `3`).
 * @param error               pipeline error message; null unless [stage] is FAILED.
 * @param stage               current [FineTuningStage] — defaults to [FineTuningStage.INITIALIZED].
 */
data class FineTuningState(
    val request: FineTuningRequest,
    val config: FineTuningConfig = FineTuningConfig(),
    val proposal: String = "",
    val trainResult: FineTuningResult? = null,
    val validationScore: Double = 0.0,
    val validationThreshold: Double = 0.7,
    val iteration: Int = 0,
    val maxIterations: Int = 3,
    val error: String? = null,
    val stage: FineTuningStage = FineTuningStage.INITIALIZED,
) {
    init {
        require(validationScore in 0.0..1.0) {
            "FineTuningState.validationScore must be in [0.0, 1.0], got $validationScore"
        }
        require(validationThreshold in 0.0..1.0) {
            "FineTuningState.validationThreshold must be in [0.0, 1.0], got $validationThreshold"
        }
        require(maxIterations > 0) {
            "FineTuningState.maxIterations must be positive, got $maxIterations"
        }
        require(iteration in 0..maxIterations) {
            "FineTuningState.iteration must be in [0, $maxIterations], got $iteration"
        }

        // proposal non-blank requires stage >= PROPOSED.
        require(!(proposal.isNotBlank() && stage < FineTuningStage.PROPOSED)) {
            "FineTuningState.proposal must be blank when stage is $stage"
        }

        // stage PROPOSED requires non-blank proposal.
        require(!(stage == FineTuningStage.PROPOSED && proposal.isBlank())) {
            "FineTuningState.proposal must not be blank when stage is PROPOSED"
        }

        // trainResult non-null requires stage >= TRAINED.
        require(!(trainResult != null && stage < FineTuningStage.TRAINED)) {
            "FineTuningState.trainResult must be null when stage is $stage"
        }

        // stage TRAINED requires trainResult non-null.
        require(!(stage == FineTuningStage.TRAINED && trainResult == null)) {
            "FineTuningState.trainResult must not be null when stage is TRAINED"
        }

        // stage VALIDATED requires trainResult non-null (Success).
        require(!(stage == FineTuningStage.VALIDATED && trainResult == null)) {
            "FineTuningState.trainResult must not be null when stage is VALIDATED"
        }

        // stage CONVERGED requires validationScore >= threshold and trainResult Success.
        require(!(stage == FineTuningStage.CONVERGED && validationScore < validationThreshold)) {
            "FineTuningState.validationScore ($validationScore) must be >= validationThreshold " +
                "($validationThreshold) when stage is CONVERGED"
        }
        require(!(stage == FineTuningStage.CONVERGED && trainResult !is FineTuningResult.Success)) {
            "FineTuningState.trainResult must be Success when stage is CONVERGED"
        }

        // error non-null requires stage FAILED.
        require(!(error != null && stage != FineTuningStage.FAILED)) {
            "FineTuningState.error must be null when stage is $stage"
        }

        // stage FAILED requires non-blank error.
        require(!(stage == FineTuningStage.FAILED && error.isNullOrBlank())) {
            "FineTuningState.error must not be blank when stage is FAILED"
        }
    }
}