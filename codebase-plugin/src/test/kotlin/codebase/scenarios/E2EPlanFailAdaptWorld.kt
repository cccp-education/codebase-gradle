package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.discovery.TaskSchema
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.planning.RollbackStrategyExecutor
import contracts.vibecoding.registry.ToolRegistry

class E2EPlanFailAdaptWorld {
    var graph: VibecodingGraph? = null
    var state: vibecoding.contracts.state.VibecodingState? = null
    var result: vibecoding.contracts.state.VibecodingState? = null
    var fakeLlm: FakeLlmProvider? = null
    var toolRegistry: ToolRegistry? = null
    var gradleCallCount: Int = 0
    var gradleResponses: MutableList<String> = mutableListOf()
    var llmResponses: MutableList<String> = mutableListOf()

    fun reset() {
        graph = null
        state = null
        result = null
        fakeLlm = null
        toolRegistry = null
        gradleCallCount = 0
        gradleResponses = mutableListOf()
        llmResponses = mutableListOf()
    }
}
