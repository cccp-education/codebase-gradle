package codebase.koog.planning

import codebase.koog.llm.LlmProvider
import codebase.koog.state.VibecodingState
import codebase.koog.tracking.TokenTracker
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

class StepVerifier(
    private val llmProvider: LlmProvider? = null,
    private val tokenTracker: TokenTracker = TokenTracker(),
    private val llmTimeoutMs: Long = DEFAULT_LLM_TIMEOUT_MS
) {

    private val log = LoggerFactory.getLogger(StepVerifier::class.java)
    private val resultVerifier = TaskResultVerifier()

    fun verifyAndAdapt(
        state: VibecodingState,
        step: VibecodingStep
    ): VibecodingState {
        val taskResult = resultVerifier.verify(state.lastToolResult, "")

        return when (taskResult.verdict) {
            TaskVerdict.SUCCESS -> {
                log.info("[StepVerifier] Step '{}' SUCCESS", step.description)
                state.copy(
                    error = null,
                    retryCount = 0
                )
            }
            TaskVerdict.FAILED -> {
                if (state.retryCount >= step.maxRetries) {
                    log.warn("[StepVerifier] Step '{}' maxRetries ({}) exhausted", step.description, step.maxRetries)
                    state.copy(
                        error = "MaxRetriesExhausted: ${taskResult.errorMessage}",
                        finished = true
                    )
                } else {
                    log.info("[StepVerifier] Step '{}' FAILED — retry {}/{}", step.description, state.retryCount + 1, step.maxRetries)
                    val replanned = if (llmProvider != null) {
                        val replanPrompt = buildReplanPrompt(state, step, taskResult)
                        tokenTracker.trackPrompt(replanPrompt)
                        try {
                            val replanResponse = runBlocking {
                                withTimeout(llmTimeoutMs) { llmProvider.call(replanPrompt) }
                            }
                            tokenTracker.trackCompletion(replanResponse)
                            log.info("[StepVerifier] Replan response: {} chars", replanResponse.length)
                            state.copy(
                                lastToolResult = "Replan: $replanResponse"
                            )
                        } catch (e: TimeoutCancellationException) {
                            log.warn("[StepVerifier] Replan LLM call timed out after {}ms", llmTimeoutMs)
                            state.copy(
                                lastToolResult = "LLMTimeout: ${llmTimeoutMs}ms exceeded"
                            )
                        } catch (e: Exception) {
                            log.warn("[StepVerifier] Replan LLM call failed: {}", e.message)
                            state
                        }
                    } else {
                        state
                    }
                    replanned.copy(
                        retryCount = state.retryCount + 1,
                        error = null
                    )
                }
            }
            TaskVerdict.BLOCKED -> {
                log.warn("[StepVerifier] Step '{}' BLOCKED: {}", step.description, taskResult.errorMessage)
                state.copy(
                    error = "BLOCKED: ${taskResult.errorMessage}",
                    finished = true
                )
            }
            TaskVerdict.UNKNOWN -> {
                log.warn("[StepVerifier] Step '{}' UNKNOWN: {}", step.description, taskResult.errorMessage)
                state.copy(
                    error = "UNKNOWN: ${taskResult.errorMessage}",
                    finished = true
                )
            }
        }
    }

    private fun buildReplanPrompt(
        state: VibecodingState,
        step: VibecodingStep,
        taskResult: TaskResult
    ): String {
        return buildString {
            appendLine("Step verification failed — retry ${state.retryCount + 1}/${step.maxRetries}")
            appendLine("Step: ${step.description} (gradle task: ${step.gradleTask})")
            appendLine("Expected: ${step.expectedOutput}")
            appendLine("Actual result: ${state.lastToolResult}")
            appendLine("Error: ${taskResult.errorMessage}")
            if (state.executedTasks.isNotEmpty()) {
                appendLine("Already executed: ${state.executedTasks.joinToString(", ")}")
            }
            appendLine()
            appendLine("The step failed. Propose an alternative approach to recover.")
            appendLine("Suggest a different Gradle task, file edit, or approach. Keep it short.")
        }
    }

    companion object {
        const val DEFAULT_LLM_TIMEOUT_MS: Long = 30_000L
    }
}
