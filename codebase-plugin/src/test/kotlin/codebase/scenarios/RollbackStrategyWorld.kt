package codebase.scenarios

import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.VibecodingPlan
import codebase.koog.state.VibecodingState

class RollbackStrategyWorld {
    var workspaceRoot: String = "/tmp/test"
    var state: VibecodingState? = null
    var plan: VibecodingPlan? = null
    var modifiedFiles: List<String> = emptyList()
    var result: VibecodingState? = null
    var executor: RollbackStrategyExecutor? = null

    fun reset() {
        workspaceRoot = "/tmp/test"
        state = null
        plan = null
        modifiedFiles = emptyList()
        result = null
        executor = null
    }
}
