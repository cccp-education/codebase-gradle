package codebase.scenarios

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.state.VibecodingState
import codebase.koog.VibecodingGraph
import contracts.agent.Epic
import contracts.agent.Plan
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.vibecoding.registry.ToolRegistry
import contracts.vibecoding.tools.ExecGradleTool

class VibeHardening2World {
    var writeFileException: IllegalArgumentException? = null
    var verifierVerdict: String? = null
    var gradleValidationException: SecurityException? = null
    var promptRemainingLine: String? = null

    var graph: VibecodingGraph = VibecodingGraph(
        augmentedGraph = null,
        toolRegistry = ToolRegistry()
    )
}