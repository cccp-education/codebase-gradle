package codebase.rag

import codebase.graph.GraphifyContextProvider
import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import contracts.context.CompositeContextConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC SUBGRAPH US-2 — Functional test of the Graphify channel wiring in
 * CompositeContextBuilder.
 *
 * Proves that with a real graph.json present, the builder replaces the old
 * regex stats with real subgraph content, while keeping the pre-existing
 * fallback when the file is absent.
 */
class CompositeContextBuilderGraphifyTest {

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
                ),
            edges =
                listOf(
                    GraphEdge("bakery/BakeryPlugin.adoc", "bakery/SiteManager.adoc", "reference"),
                ),
            communities =
                listOf(
                    GraphCommunity("bakery-gradle", "Bakery Gradle Plugin", 2),
                ),
        )

    private fun writeOfficeGraph() {
        val office = tempDir.resolve("office").apply { mkdirs() }
        office.resolve("graph.json").writeText(mapper.writeValueAsString(syntheticGraph))
    }

    private fun newBuilder(): CompositeContextBuilder {
        val store = VectorStore("jdbc:postgresql://localhost:1/codebase_test", "nope", "nope")
        val pipeline = EmbeddingPipeline(store)
        val config = CompositeContextConfig()
        return CompositeContextBuilder(tempDir, store, pipeline, config)
    }

    @Test
    fun `graphify section contains real subgraph content when graph json exists`() {
        writeOfficeGraph()
        val composite = newBuilder().buildScoped("test-borough", "context")

        assertThat(composite.graphifySection).contains("[Graphify] subgraph:")
        assertThat(composite.graphifySection).contains("bakery/BakeryPlugin.adoc")
        assertThat(composite.graphifySection).contains("->")
    }

    @Test
    fun `graphify section keeps the fallback message when graph json is absent`() {
        val composite = newBuilder().buildScoped("test-borough", "context")

        assertThat(composite.graphifySection).isEqualTo("[Graphify] graph.json non trouve dans office/")
    }

    @Test
    fun `graphify section is truncable to the configured budget`() {
        writeOfficeGraph()
        val config = CompositeContextConfig()
        val store = VectorStore("jdbc:postgresql://localhost:1/codebase_test", "nope", "nope")
        val builder = CompositeContextBuilder(tempDir, store, EmbeddingPipeline(store), config)
        val composite = builder.buildScoped("test-borough", "context")

        val maxChars = config.graphifyTokens * 4
        assertThat(composite.graphifySection.length).isLessThanOrEqualTo(maxChars + 10)
    }
}