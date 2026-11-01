package codebase.quality

data class QualityAttempt(
    val attempt: Int,
    val output: String,
    val prompt: String,
    val qualityAssessment: QualityAssessment
)

data class QualityResult(
    val bestOutput: String?,
    val attempts: Int,
    val passed: Boolean,
    val history: List<QualityAttempt>,
    val summary: String
)

/**
 * Bounded quality-retry circuit.
 *
 * XB-FP-5 (arrow-kt adoption): the loop used three mutable variables
 * (`currentPrompt`, `best`, `history`) and an imperative `for` with early
 * `return`. It is now a `fold` over the attempt range carrying an immutable
 * [CircuitState] — no `var`, no `MutableList`, no early return. The `fold` is
 * inline, so the suspend LLM call is still allowed; a passed state short-circuits
 * the remaining attempts by returning the accumulated state unchanged.
 */
class QualityRetryCircuit(
    private val llm: suspend (String) -> String,
    private val gate: QualityGate,
    private val domain: Domain,
    private val maxRetries: Int = gate.config.maxRetries
) {

    /** Immutable state carried from one attempt to the next. */
    private data class CircuitState(
        val prompt: String,
        val history: List<QualityAttempt>,
        val firstOutput: String?,
        val passed: Boolean
    )

    suspend fun invoke(initialPrompt: String): QualityResult {
        val totalAttempts = maxRetries + 1

        val finalState = (1..totalAttempts).fold(
            CircuitState(prompt = initialPrompt, history = emptyList(), firstOutput = null, passed = false)
        ) { state, attempt ->
            if (state.passed) {
                return@fold state
            }

            val output = llm(state.prompt)
            val assessment = gate.evaluate(output, domain)
            val record = QualityAttempt(
                attempt = attempt,
                output = output,
                prompt = state.prompt,
                qualityAssessment = assessment
            )
            val nextPrompt =
                if (assessment.passed || attempt == totalAttempts) {
                    state.prompt
                } else {
                    "$initialPrompt\n\n${gate.buildFeedback(assessment)}"
                }

            CircuitState(
                prompt = nextPrompt,
                history = state.history + record,
                firstOutput = state.firstOutput ?: output,
                passed = assessment.passed
            )
        }

        return if (finalState.passed) {
            val passing = finalState.history.last()
            QualityResult(
                bestOutput = passing.output,
                attempts = passing.attempt,
                passed = true,
                history = finalState.history,
                summary = "GATE: PASS — ${passing.attempt} attempt(s)"
            )
        } else {
            val bestAttempt = finalState.history
                .maxByOrNull { it.qualityAssessment.results.sumOf { r -> (r.score * 1000).toLong() } }
            QualityResult(
                bestOutput = bestAttempt?.output ?: finalState.firstOutput,
                attempts = totalAttempts,
                passed = false,
                history = finalState.history,
                summary = "GATE: FAIL — $totalAttempts attempt(s)"
            )
        }
    }
}
