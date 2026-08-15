package codebase.graph

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC SUBGRAPH US-2 — Unit tests for GraphifyContextProvider.
 *
 * The provider turns a graph.json file into the Graphify channel content:
 * load → extract (LensConfig) → render (GraphContextRenderer), with the
 * pre-existing resilient fallbacks when the file is missing or unreadable.
 *
 * Methodology: DDD/TDD baby steps — RED (unresolved reference) → GREEN.
 */
class GraphifyContextProviderTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = ObjectMapper().registerKotlinModule()

    private val syntheticGraph =
        GraphModel(
            nodes =
                listOf(
                    GraphNode("bakery/BakeryPlugin.adoc", "BakeryPlugin.adoc", "file", "bakery-gradle"),
                    GraphNode("bakery/SiteManager.adoc", "SiteManager.adoc", "file", "bakery-gradle"),
                    GraphNode("bakery-gradle", "bakery-gradle", "module", "bakery-gradle"),
                    GraphNode("codebase/RagService.adoc", "RagService.adoc", "file", "codebase-gradle"),
                ),
            edges =
                listOf(
                    GraphEdge("bakery/BakeryPlugin.adoc", "bakery/SiteManager.adoc", "reference"),
                    GraphEdge("bakery/BakeryPlugin.adoc", "bakery-gradle", "belongs_to"),
                ),
            communities =
                listOf(
                    GraphCommunity("bakery-gradle", "Bakery Gradle Plugin", 3),
                ),
        )

    private fun writeGraph(graph: GraphModel = syntheticGraph): File {
        val file = tempDir.resolve("graph.json")
        file.writeText(mapper.writeValueAsString(graph))
        return file
    }

    @Test
    fun `missing graph file returns the existing fallback message`() {
        val provider = GraphifyContextProvider(tempDir.resolve("office/graph.json"))
        assertThat(provider.provide()).isEqualTo("[Graphify] graph.json non trouve dans office/")
    }

    @Test
    fun `valid graph file renders structured subgraph content`() {
        val file = writeGraph()
        val provider = GraphifyContextProvider(file, LensConfig(communities = listOf("bakery-gradle")))
        val text = provider.provide()
        assertThat(text).contains("[Graphify] subgraph: 2 nodes, 1 edges, 1 communities")
        assertThat(text).contains("- node bakery/BakeryPlugin.adoc [type=file, community=bakery-gradle]")
        assertThat(text).contains("- edge bakery/BakeryPlugin.adoc -> bakery/SiteManager.adoc [type=reference]")
        assertThat(text).contains("- community bakery-gradle (3 nodes)")
    }

    @Test
    fun `unreadable graph file returns the illisible fallback message`() {
        val malformed = tempDir.resolve("graph.json").apply { writeText("this is not valid json") }
        val provider = GraphifyContextProvider(malformed)
        val text = provider.provide()
        assertThat(text).startsWith("[Graphify] graph.json illisible:")
    }

    @Test
    fun `full scope renders the whole graph`() {
        val file = writeGraph()
        val provider = GraphifyContextProvider(file, LensConfig(scope = LensScope.FULL))
        val text = provider.provide()
        assertThat(text).contains("4 nodes")
        assertThat(text).contains("- node bakery-gradle [type=module, community=bakery-gradle]")
    }

    @Test
    fun `semantic only scope produces an empty subgraph`() {
        val file = writeGraph()
        val provider = GraphifyContextProvider(file, LensConfig(scope = LensScope.SEMANTIC_ONLY))
        assertThat(provider.provide()).isEqualTo("[Graphify] subgraph: 0 nodes, 0 edges, 0 communities")
    }
}