package codebase.graph

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * EPIC SUBGRAPH US-2 — Unit tests for GraphContextRenderer.
 *
 * The renderer converts a [SiteSubgraph] into the structured text injected
 * into the Graphify context channel (nodes, edges, communities).
 *
 * Methodology: DDD/TDD baby steps — RED (unresolved reference) → GREEN.
 */
class GraphContextRendererTest {

    private val subgraph =
        SiteSubgraph(
            nodes =
                listOf(
                    GraphNode("codebase/RagService.kt", "RagService.kt", "file", "codebase-gradle"),
                    GraphNode("codebase/VectorStore.kt", "VectorStore.kt", "file", "codebase-gradle"),
                ),
            edges =
                listOf(
                    GraphEdge("codebase/RagService.kt", "codebase/VectorStore.kt", "reference"),
                ),
            communities =
                listOf(
                    GraphCommunity("codebase-gradle", "Codebase Gradle Plugin", 2),
                ),
        )

    @Test
    fun `render includes subgraph header with counts`() {
        val text = GraphContextRenderer.render(subgraph)
        assertThat(text).contains("[Graphify] subgraph: 2 nodes, 1 edges, 1 communities")
    }

    @Test
    fun `render lists each node with id type and community`() {
        val text = GraphContextRenderer.render(subgraph)
        assertThat(text).contains("- node codebase/RagService.kt [type=file, community=codebase-gradle]")
        assertThat(text).contains("- node codebase/VectorStore.kt [type=file, community=codebase-gradle]")
    }

    @Test
    fun `render lists each edge with source target and type`() {
        val text = GraphContextRenderer.render(subgraph)
        assertThat(text).contains("- edge codebase/RagService.kt -> codebase/VectorStore.kt [type=reference]")
    }

    @Test
    fun `render lists communities with node counts`() {
        val text = GraphContextRenderer.render(subgraph)
        assertThat(text).contains("- community codebase-gradle (2 nodes)")
    }

    @Test
    fun `render empty subgraph produces zero counts and no entries`() {
        val text = GraphContextRenderer.render(SiteSubgraph(emptyList(), emptyList(), emptyList()))
        assertThat(text).contains("[Graphify] subgraph: 0 nodes, 0 edges, 0 communities")
        assertThat(text).doesNotContain("- node")
        assertThat(text).doesNotContain("- edge")
        assertThat(text).doesNotContain("- community")
    }

    @Test
    fun `render is deterministic for identical subgraphs`() {
        val first = GraphContextRenderer.render(subgraph)
        val second = GraphContextRenderer.render(subgraph)
        assertThat(first).isEqualTo(second)
    }
}