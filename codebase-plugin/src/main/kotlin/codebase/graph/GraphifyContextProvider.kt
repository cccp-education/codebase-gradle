package codebase.graph

import java.io.File

/**
 * Provides the Graphify context channel content from a graph.json file
 * (EPIC SUBGRAPH US-2).
 *
 * Pipeline: load graph.json → extract subgraph ([LensConfig]) → render
 * structured text ([GraphContextRenderer]).
 *
 * Keeps the pre-existing resilient fallbacks of the old loadGraphifyStats:
 * missing file → "[Graphify] graph.json non trouve dans office/",
 * unreadable file → "[Graphify] graph.json illisible: <message>".
 * These strings are a backward-compat contract asserted by existing tests.
 *
 * File I/O only — no Gradle, no network. Injectable [File] + [LensConfig]
 * for full testability.
 */
class GraphifyContextProvider(
    private val graphFile: File,
    private val lensConfig: LensConfig = LensConfig(),
) {
    private val extractor = SubgraphExtractor()

    fun provide(): String {
        if (!graphFile.isFile) return "[Graphify] graph.json non trouve dans office/"

        val graphModel =
            try {
                extractor.loadGraph(graphFile.absolutePath)
            } catch (e: Exception) {
                return "[Graphify] graph.json illisible: ${e.message}"
            }

        val subgraph = extractor.extract(graphModel, lensConfig)
        return GraphContextRenderer.render(subgraph)
    }
}