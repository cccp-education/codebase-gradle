package codebase.scenarios

import codebase.koog.planning.RollbackStrategy
import codebase.koog.planning.VibecodingPlan
import codebase.koog.planning.VibecodingStep

class VibecodingPlanWorld {
    var steps: MutableList<VibecodingStep> = mutableListOf()
    var strategy: RollbackStrategy = RollbackStrategy.STOP_ON_ERROR
    var plan: VibecodingPlan? = null

    fun reset() {
        steps = mutableListOf()
        strategy = RollbackStrategy.STOP_ON_ERROR
        plan = null
    }

    fun build() {
        plan = VibecodingPlan(steps.toList(), rollbackStrategy = strategy)
    }
}
