package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.discovery.TaskSchema
import codebase.koog.state.VibecodingState

class ReplanCatalogWorld {
    var graph: VibecodingGraph? = null
    var state: VibecodingState? = null
    var capturedPrompt: String? = null
    var taskSchemas: List<TaskSchema> = emptyList()

    fun reset() {
        graph = null
        state = null
        capturedPrompt = null
        taskSchemas = emptyList()
    }
}
