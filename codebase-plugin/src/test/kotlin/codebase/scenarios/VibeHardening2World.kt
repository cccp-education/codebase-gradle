package codebase.scenarios

import codebase.koog.VibecodingGraph
import contracts.vibecoding.registry.ToolRegistry

class VibeHardening2World {
    var writeFileException: IllegalArgumentException? = null
    var verifierVerdict: String? = null
    var gradleValidationException: SecurityException? = null
    var promptRemainingLine: String? = null

    var graph: VibecodingGraph =
        VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
        )
}
