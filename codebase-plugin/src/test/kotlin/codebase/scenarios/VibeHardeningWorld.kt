package codebase.scenarios

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import codebase.koog.state.VibecodingState
import codebase.koog.VibecodingGraph
import contracts.vibecoding.registry.ToolRegistry

class VibeHardeningWorld {

    var resultState: VibecodingState? = null
    var graph: VibecodingGraph = VibecodingGraph(
        augmentedGraph = null,
        toolRegistry = ToolRegistry()
    )

    var shellValidationException: SecurityException? = null
    var gradleValidationException: SecurityException? = null

    var slowLlmProvider: LlmProvider? = null
    var slowLlmSleepMs: Int = 0

    var fakeLlmProvider: FakeLlmProvider? = null

    fun initGraphWithFakeLLM() {
        val provider = FakeLlmProvider()
        fakeLlmProvider = provider
        graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = provider
        )
    }
}