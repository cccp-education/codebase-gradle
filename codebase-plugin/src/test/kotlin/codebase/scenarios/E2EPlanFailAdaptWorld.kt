package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.state.VibecodingState
import contracts.vibecoding.registry.ToolRegistry

class E2EPlanFailAdaptWorld {
    var graph: VibecodingGraph? = null
    var state: VibecodingState? = null
    var result: VibecodingState? = null
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
