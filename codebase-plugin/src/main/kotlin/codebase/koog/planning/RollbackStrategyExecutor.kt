package codebase.koog.planning

import codebase.koog.state.VibecodingState
import contracts.vibecoding.registry.ToolRegistry
import org.slf4j.LoggerFactory

class RollbackStrategyExecutor(
    private val toolRegistry: ToolRegistry,
    private val workspaceRoot: String
) {

    private val log = LoggerFactory.getLogger(RollbackStrategyExecutor::class.java)

    fun execute(
        state: VibecodingState,
        plan: VibecodingPlan,
        failedStep: VibecodingStep,
        modifiedFiles: List<String> = emptyList()
    ): VibecodingState {
        return when (plan.rollbackStrategy) {
            RollbackStrategy.STOP_ON_ERROR -> executeStopOnError(state, failedStep)
            RollbackStrategy.REVERT_AND_CONTINUE -> executeRevertAndContinue(state, failedStep, modifiedFiles)
            RollbackStrategy.MARK_SKIPPED -> executeMarkSkipped(state, failedStep)
            RollbackStrategy.FALLBACK_HUMAN -> executeFallbackHuman(state, failedStep)
        }
    }

    private fun executeStopOnError(
        state: VibecodingState,
        failedStep: VibecodingStep
    ): VibecodingState {
        log.warn("[RollbackStrategy] STOP_ON_ERROR — step '{}' failed, stopping", failedStep.description)
        return state.copy(
            finished = true,
            error = "STOP_ON_ERROR: step '${failedStep.description}' failed after maxRetries"
        )
    }

    private fun executeRevertAndContinue(
        state: VibecodingState,
        failedStep: VibecodingStep,
        modifiedFiles: List<String>
    ): VibecodingState {
        log.info("[RollbackStrategy] REVERT_AND_CONTINUE — reverting step '{}'", failedStep.description)

        if (modifiedFiles.isNotEmpty()) {
            val filesArg = modifiedFiles.joinToString(" ")
            try {
                toolRegistry.execute(
                    toolName = "exec_shell",
                    arguments = mapOf("command" to "git checkout -- $filesArg"),
                    workspaceRoot = workspaceRoot
                )
                log.info("[RollbackStrategy] Reverted {} files", modifiedFiles.size)
            } catch (e: Exception) {
                log.warn("[RollbackStrategy] git checkout failed: {}", e.message)
            }
        }

        return state.copy(
            error = null,
            retryCount = 0,
            finished = false,
            lastToolResult = "REVERT_AND_CONTINUE: step '${failedStep.description}' reverted, continuing"
        )
    }

    private fun executeMarkSkipped(
        state: VibecodingState,
        failedStep: VibecodingStep
    ): VibecodingState {
        log.info("[RollbackStrategy] MARK_SKIPPED — skipping step '{}'", failedStep.description)
        return state.copy(
            error = null,
            retryCount = 0,
            finished = false,
            executedTasks = state.executedTasks + failedStep.description,
            lastToolResult = "SKIPPED: step '${failedStep.description}' marked as skipped"
        )
    }

    private fun executeFallbackHuman(
        state: VibecodingState,
        failedStep: VibecodingStep
    ): VibecodingState {
        log.warn("[RollbackStrategy] FALLBACK_HUMAN — requesting human input for step '{}'", failedStep.description)
        return state.copy(
            finished = true,
            error = "FALLBACK_HUMAN: step '${failedStep.description}' requires human intervention. " +
                "Please review the error and provide instructions."
        )
    }
}
