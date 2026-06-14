package codebase.koog.planning

enum class RollbackStrategy {
    STOP_ON_ERROR,
    REVERT_AND_CONTINUE,
    MARK_SKIPPED,
    FALLBACK_HUMAN
}

data class VibecodingStep(
    val description: String,
    val gradleTask: String,
    val expectedOutput: String,
    val maxRetries: Int = 3,
    val verifyHook: String? = null
)

data class VibecodingPlan(
    val steps: List<VibecodingStep>,
    val rollbackStrategy: RollbackStrategy = RollbackStrategy.STOP_ON_ERROR
)
