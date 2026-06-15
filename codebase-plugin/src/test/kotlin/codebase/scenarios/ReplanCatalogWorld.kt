package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.discovery.TaskSchema
import codebase.koog.discovery.TaskOption
import codebase.koog.llm.LlmProvider
import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.StepVerifier
import contracts.vibecoding.registry.ToolRegistry

class ReplanCatalogWorld {
    var graph: VibecodingGraph? = null
    var state: vibecoding.contracts.state.VibecodingState? = null
    var capturedPrompt: String? = null
    var taskSchemas: List<TaskSchema> = emptyList()

    fun reset() {
        graph = null
        state = null
        capturedPrompt = null
        taskSchemas = emptyList()
    }
}
