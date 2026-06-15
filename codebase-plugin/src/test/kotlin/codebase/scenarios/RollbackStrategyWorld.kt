package codebase.scenarios

import codebase.koog.planning.RollbackStrategy
import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.VibecodingPlan
import codebase.koog.planning.VibecodingStep
import contracts.vibecoding.registry.ToolRegistry

class RollbackStrategyWorld {
    var workspaceRoot: String = "/tmp/test"
    var state: vibecoding.contracts.state.VibecodingState? = null
    var plan: VibecodingPlan? = null
    var modifiedFiles: List<String> = emptyList()
    var result: vibecoding.contracts.state.VibecodingState? = null
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
