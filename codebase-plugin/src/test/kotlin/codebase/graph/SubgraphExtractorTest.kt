package codebase.graph

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * EPIC SUBGRAPH US-1 — Unit tests for SubgraphExtractor.
 *
 * Ported from bakery SubgraphExtractorTest (BKY-LENS-1.3/1.4) into the
 * shared N1 socle codebase.graph. Fixture: in-memory graph with 5
 * communities (bakery-gradle, codebase-gradle, slider-gradle,
 * workspace-bom, graphify-gradle) + 1 orphan node.
 *
 * Methodology: DDD/TDD baby steps — each test compiles AND passes before
 * moving to the next one.
 */
class SubgraphExtractorTest {
    private lateinit var extractor: SubgraphExtractor

    @TempDir
    lateinit var tempDir: File

    // ─── In-memory test graph ───

    /** Nodes of the fixture (communities: bakery-gradle, codebase-gradle, slider-gradle, workspace-bom, graphify-gradle) */
    private val bakeryNodes =
        listOf(
            GraphNode("bakery/BakeryPlugin.kt", "BakeryPlugin.kt", "file", "bakery-gradle"),
            GraphNode("bakery/SiteManager.kt", "SiteManager.kt", "file", "bakery-gradle"),
            GraphNode("bakery/BakeryExtension.kt", "BakeryExtension.kt", "file", "bakery-gradle"),
            GraphNode("bakery/LensConfig.kt", "LensConfig.kt", "file", "bakery-gradle"),
            GraphNode("bakery/SubgraphExtractor.kt", "SubgraphExtractor.kt", "file", "bakery-gradle"),
            GraphNode("bakery/post.thyme", "post.thyme", "file", "bakery-gradle"),
            GraphNode("bakery/docs/index.adoc", "index.adoc", "file", "bakery-gradle"),
            GraphNode("bakery/docs/getting-started.adoc", "getting-started.adoc", "file", "bakery-gradle"),
            GraphNode("bakery/README.adoc", "README.adoc", "file", "bakery-gradle"),
            GraphNode("bakery-gradle", "bakery-gradle", "module", "bakery-gradle"),
        )

    private val codebaseNodes =
        listOf(
            GraphNode("codebase/RagService.kt", "RagService.kt", "file", "codebase-gradle"),
            GraphNode("codebase/CodebasePlugin.kt", "CodebasePlugin.kt", "file", "codebase-gradle"),
            GraphNode("codebase/VectorStore.kt", "VectorStore.kt", "file", "codebase-gradle"),
            GraphNode("codebase/docs/architecture.adoc", "architecture.adoc", "file", "codebase-gradle"),
            GraphNode("codebase-gradle", "codebase-gradle", "module", "codebase-gradle"),
        )

    private val sliderNodes =
        listOf(
            GraphNode("slider/SliderPlugin.kt", "SliderPlugin.kt", "file", "slider-gradle"),
            GraphNode("slider/RevealJsTask.kt", "RevealJsTask.kt", "file", "slider-gradle"),
            GraphNode("slider-gradle", "slider-gradle", "module", "slider-gradle"),
        )

    private val bomNodes =
        listOf(
            GraphNode("bom/CompositeContext.kt", "CompositeContext.kt", "file", "workspace-bom"),
            GraphNode("bom/ContextChannel.kt", "ContextChannel.kt", "file", "workspace-bom"),
            GraphNode("bom/ChannelBudget.kt", "ChannelBudget.kt", "module", "workspace-bom"),
            GraphNode("workspace-bom", "workspace-bom", "module", "workspace-bom"),
        )

    private val graphifyNodes =
        listOf(
            GraphNode("graphify/GraphifyPlugin.kt", "GraphifyPlugin.kt", "file", "graphify-gradle"),
            GraphNode("graphify/GraphModel.kt", "GraphModel.kt", "file", "graphify-gradle"),
            GraphNode("graphify-gradle", "graphify-gradle", "module", "graphify-gradle"),
        )

    private val orphanNode = GraphNode("orphan.adoc", "orphan.adoc", "file", community = null)

    private val allNodes: List<GraphNode>
        get() = bakeryNodes + codebaseNodes + sliderNodes + bomNodes + graphifyNodes + orphanNode

    private val allEdges =
        listOf(
            // Internal bakery references
            GraphEdge("bakery/BakeryPlugin.kt", "bakery/SiteManager.kt", "reference"),
            GraphEdge("bakery/BakeryPlugin.kt", "bakery/BakeryExtension.kt", "reference"),
            GraphEdge("bakery/SiteManager.kt", "bakery/LensConfig.kt", "reference"),
            GraphEdge("bakery/SubgraphExtractor.kt", "bakery/LensConfig.kt", "reference"),
            // Cross-borough bakery -> bom
            GraphEdge("bakery/SubgraphExtractor.kt", "bom/CompositeContext.kt", "reference"),
            GraphEdge("bakery/LensConfig.kt", "bom/ContextChannel.kt", "reference"),
            // Cross-borough codebase -> bom
            GraphEdge("codebase/RagService.kt", "bom/CompositeContext.kt", "reference"),
            GraphEdge("codebase/VectorStore.kt", "bom/ChannelBudget.kt", "reference"),
            // Cross-borough slider -> bom + bakery
            GraphEdge("slider/SliderPlugin.kt", "bom/ContextChannel.kt", "reference"),
            // Contains edges
            GraphEdge("bakery-gradle", "bakery/BakeryPlugin.kt", "contains"),
            GraphEdge("codebase-gradle", "codebase/CodebasePlugin.kt", "contains"),
            GraphEdge("slider-gradle", "slider/SliderPlugin.kt", "contains"),
            // Agent reference (cross-community)
            GraphEdge("bakery/docs/index.adoc", "bakery/docs/getting-started.adoc", "agent_reference"),
            GraphEdge("bakery/docs/index.adoc", "codebase/docs/architecture.adoc", "agent_reference"),
            // Graphify internal
            GraphEdge("graphify/GraphModel.kt", "graphify-gradle", "contains"),
            // Orphan
            GraphEdge("orphan.adoc", "bakery/docs/index.adoc", "reference"),
        )

    private val allCommunities =
        listOf(
            GraphCommunity("bakery-gradle", "Bakery Gradle Plugin", 10),
            GraphCommunity("codebase-gradle", "Codebase Gradle Plugin", 5),
            GraphCommunity("slider-gradle", "Slider Gradle Plugin", 3),
            GraphCommunity("workspace-bom", "Workspace BOM", 4),
            GraphCommunity("graphify-gradle", "Graphify Gradle Plugin", 3),
        )

    private val testGraphModel: GraphModel
        get() =
            GraphModel(
                nodes = allNodes,
                edges = allEdges,
                communities = allCommunities,
            )

    @BeforeEach
    fun setUp() {
        extractor = SubgraphExtractor()
    }

    // ──────────────────────────────────────
    // Community filtering
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — community filtering")
    inner class CommunityFiltering {
        @Test
        @DisplayName("filter a single community — bakery-gradle")
        fun `filter single community bakery-gradle`() {
            val config = LensConfig(communities = listOf("bakery-gradle"))
            val result = extractor.extract(testGraphModel, config)

            // bakery nodes only (bakery community + orphans included)
            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.map { it.community }.distinct()).containsExactly("bakery-gradle", null)
            assertThat(result.communityIds).containsExactly("bakery-gradle")
        }

        @Test
        @DisplayName("filter multiple communities — bakery + codebase")
        fun `filter multiple communities`() {
            val config = LensConfig(communities = listOf("bakery-gradle", "codebase-gradle"))
            val result = extractor.extract(testGraphModel, config)

            val communityIds = result.nodes.mapNotNull { it.community }.distinct()
            assertThat(communityIds).containsExactlyInAnyOrder("bakery-gradle", "codebase-gradle")
        }

        @Test
        @DisplayName("empty communities = no community filtering (all nodes)")
        fun `empty communities includes all nodes matching type filter`() {
            val config = LensConfig(communities = emptyList(), nodeTypes = listOf("file"), fileExtensions = emptyList())
            val result = extractor.extract(testGraphModel, config)

            // No community filter = all files pass
            assertThat(result.nodes.size).isGreaterThan(10)
        }
    }

    // ──────────────────────────────────────
    // Node type filtering
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — node type filtering")
    inner class NodeTypeFiltering {
        @Test
        @DisplayName("filter type = file only (exclude modules)")
        fun `filter file type only excludes modules`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                )
            val result = extractor.extract(testGraphModel, config)

            // No "module" node in the result
            assertThat(result.nodes.none { it.type == "module" }).isTrue
            // All nodes are "file"
            assertThat(result.nodes.all { it.type == "file" }).isTrue
        }

        @Test
        @DisplayName("filter type = module only")
        fun `filter module type only`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("module"),
                )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes.map { it.type }).containsExactly("module")
        }
    }

    // ──────────────────────────────────────
    // Edge type filtering
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — edge type filtering")
    inner class EdgeTypeFiltering {
        @Test
        @DisplayName("filter reference edges only (exclude contains)")
        fun `filter reference edges only`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle", "workspace-bom"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("reference"),
                )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.edges.all { it.type == "reference" }).isTrue
            assertThat(result.edges.none { it.type == "contains" }).isTrue
        }

        @Test
        @DisplayName("filter agent_reference edges only")
        fun `filter agent_reference edges only`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle", "codebase-gradle"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("agent_reference"),
                )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.edges.all { it.type == "agent_reference" }).isTrue
        }

        @Test
        @DisplayName("empty edge types = all edge types included")
        fun `empty edge types includes all edge types`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle", "workspace-bom"),
                    nodeTypes = listOf("file", "module"),
                    edgeTypes = emptyList(),
                )
            val result = extractor.extract(testGraphModel, config)

            val edgeTypes = result.edges.map { it.type }.distinct()
            assertThat(edgeTypes).containsAnyOf("reference", "contains", "agent_reference")
        }
    }

    // ──────────────────────────────────────
    // File extension filtering
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — file extension filtering")
    inner class FileExtensionFiltering {
        @Test
        @DisplayName("filter .kt files only")
        fun `filter kt files only`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    fileExtensions = listOf("kt"),
                )
            val result = extractor.extract(testGraphModel, config)

            // All file nodes must have the .kt extension
            assertThat(result.nodes.all { it.id.endsWith(".kt") || it.type != "file" }).isTrue
        }

        @Test
        @DisplayName("filter .adoc files only")
        fun `filter adoc files only`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    fileExtensions = listOf("adoc"),
                )
            val result = extractor.extract(testGraphModel, config)

            // All file nodes must have the .adoc extension
            assertThat(result.nodes.all { it.id.endsWith(".adoc") || it.type != "file" }).isTrue
        }

        @Test
        @DisplayName("filter .kt + .adoc files")
        fun `filter kt and adoc files`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    fileExtensions = listOf("kt", "adoc"),
                )
            val result = extractor.extract(testGraphModel, config)

            val extensions =
                result.nodes
                    .filter { it.type == "file" }
                    .map { it.id.substringAfterLast('.', "") }
                    .distinct()
            assertThat(extensions).isSubsetOf("kt", "adoc")
        }
    }

    // ──────────────────────────────────────
    // BFS depth
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — BFS depth (maxDepth)")
    inner class BfsDepth {
        @Test
        @DisplayName("maxDepth=0 : only seed community nodes")
        fun `maxDepth 0 returns only seed nodes`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("reference"),
                    maxDepth = 0,
                )
            val result = extractor.extract(testGraphModel, config)

            // With maxDepth=0, only target-community nodes are kept
            // (no expansion towards neighboring communities)
            val bakeryFileIds = bakeryNodes.filter { it.type == "file" }.map { it.id }.toSet()
            result.nodes.forEach { node ->
                assertThat(node.community).isIn("bakery-gradle", null)
            }
        }

        @Test
        @DisplayName("maxDepth=1 : seeds + direct neighbors (1 hop)")
        fun `maxDepth 1 expands to direct neighbors`() {
            val configDepth0 =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("reference"),
                    maxDepth = 0,
                )
            val result0 = extractor.extract(testGraphModel, configDepth0)

            val configDepth1 = configDepth0.copy(maxDepth = 1)
            val result1 = extractor.extract(testGraphModel, configDepth1)

            // Depth 1 must have at least as many nodes as depth 0
            // and potentially more (cross-community neighbors)
            assertThat(result1.nodes.size).isGreaterThanOrEqualTo(result0.nodes.size)
        }

        @Test
        @DisplayName("maxDepth=2 : seeds + neighbors + neighbors of neighbors")
        fun `maxDepth 2 expands further`() {
            val configDepth1 =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("reference"),
                    maxDepth = 1,
                )
            val result1 = extractor.extract(testGraphModel, configDepth1)

            val configDepth2 = configDepth1.copy(maxDepth = 2)
            val result2 = extractor.extract(testGraphModel, configDepth2)

            // Depth 2 >= depth 1
            assertThat(result2.nodes.size).isGreaterThanOrEqualTo(result1.nodes.size)
        }
    }

    // ──────────────────────────────────────
    // Scopes (SUBGRAPH, FULL, SEMANTIC_ONLY)
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — scope (LensScope)")
    inner class ScopeSelection {
        @Test
        @DisplayName("FULL scope returns the entire graph")
        fun `FULL scope returns entire graph`() {
            val config = LensConfig(scope = LensScope.FULL)
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes.size).isEqualTo(allNodes.size)
            assertThat(result.edges.size).isEqualTo(allEdges.size)
        }

        @Test
        @DisplayName("SEMANTIC_ONLY scope returns empty subgraph (no graph)")
        fun `SEMANTIC_ONLY scope returns empty subgraph`() {
            val config = LensConfig(scope = LensScope.SEMANTIC_ONLY)
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes).isEmpty()
            assertThat(result.edges).isEmpty()
            assertThat(result.communities).isEmpty()
        }

        @Test
        @DisplayName("SUBGRAPH scope applies normal filtering")
        fun `SUBGRAPH scope applies normal filtering`() {
            val config =
                LensConfig(
                    scope = LensScope.SUBGRAPH,
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                )
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.map { it.community }.distinct()).containsExactly("bakery-gradle", null)
        }
    }

    // ──────────────────────────────────────
    // Orphan nodes (community=null)
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — orphan nodes (community=null)")
    inner class OrphanNodes {
        @Test
        @DisplayName("nodes without community are included by default")
        fun `orphan nodes are included by default`() {
            val config = LensConfig(communities = listOf("bakery-gradle"), nodeTypes = listOf("file"))
            val result = extractor.extract(testGraphModel, config)

            // The orphan "orphan.adoc" (community=null) must be included
            assertThat(result.nodes.any { it.community == null }).isTrue
        }
    }

    // ──────────────────────────────────────
    // SiteSubgraph — utility operations
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — SiteSubgraph utilities")
    inner class SiteSubgraphUtils {
        @Test
        @DisplayName("nodeCount, edgeCount, communityCount")
        fun `subgraph counts`() {
            val config = LensConfig(communities = listOf("bakery-gradle"))
            val result = extractor.extract(testGraphModel, config)

            assertThat(result.nodeCount).isEqualTo(result.nodes.size)
            assertThat(result.edgeCount).isEqualTo(result.edges.size)
            assertThat(result.communityCount).isEqualTo(result.communities.size)
        }

        @Test
        @DisplayName("nodesInCommunity returns nodes of a given community")
        fun `nodesInCommunity returns nodes for a given community`() {
            val config = LensConfig(communities = listOf("bakery-gradle"), nodeTypes = listOf("file"))
            val result = extractor.extract(testGraphModel, config)

            val bakeryNodes = result.nodesInCommunity("bakery-gradle")
            assertThat(bakeryNodes).isNotEmpty
            assertThat(bakeryNodes.all { it.community == "bakery-gradle" }).isTrue
        }

        @Test
        @DisplayName("edgesOfType returns edges of a given type")
        fun `edgesOfType returns edges for a given type`() {
            val config = LensConfig(communities = listOf("bakery-gradle", "workspace-bom"), edgeTypes = listOf("reference"))
            val result = extractor.extract(testGraphModel, config)

            val references = result.edgesOfType("reference")
            assertThat(references).isNotEmpty
            assertThat(references.all { it.type == "reference" }).isTrue
        }

        @Test
        @DisplayName("neighbors returns nodes connected to a given node")
        fun `neighbors returns nodes connected to a given node`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file", "module"),
                    edgeTypes = listOf("reference"),
                )
            val result = extractor.extract(testGraphModel, config)

            // BakeryPlugin.kt has reference neighbors inside bakery-gradle
            val bakeryPlugin = result.nodes.find { it.id == "bakery/BakeryPlugin.kt" }
            if (bakeryPlugin != null) {
                val neighbors = result.neighbors("bakery/BakeryPlugin.kt")
                assertThat(neighbors).isNotEmpty
            }
        }
    }

    // ──────────────────────────────────────
    // Loading graph.json from file
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — loading from file")
    inner class FileLoading {
        @Test
        @DisplayName("loadGraph returns empty graph for non-existent file")
        fun `loadGraph returns empty for non-existent file`() {
            val result = extractor.loadGraph("/nonexistent/graph.json")
            assertThat(result.nodes).isEmpty()
            assertThat(result.edges).isEmpty()
            assertThat(result.communities).isEmpty()
        }

        @Test
        @DisplayName("extractFromPath loads and filters from a JSON file")
        fun `extractFromPath loads and filters from JSON file`() {
            // Write the fixture
            val graphFile = File(tempDir, "test-graph.json")
            val objectMapper =
                com.fasterxml.jackson.module.kotlin
                    .jacksonObjectMapper()
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(graphFile, testGraphModel)

            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                )
            val result = extractor.extractFromPath(graphFile.absolutePath, config)

            assertThat(result.nodes).isNotEmpty
            assertThat(result.nodes.mapNotNull { it.community }.distinct()).containsExactly("bakery-gradle")
        }
    }

    // ──────────────────────────────────────
    // Cross-community edges
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — cross-community edges")
    inner class CrossCommunityEdges {
        @Test
        @DisplayName("cross-community edges are preserved when both endpoints are in the subgraph")
        fun `cross-community edges preserved when both endpoints in subgraph`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle", "workspace-bom"),
                    nodeTypes = listOf("file", "module"),
                    edgeTypes = listOf("reference"),
                )
            val result = extractor.extract(testGraphModel, config)

            // bakery->bom edges must be preserved
            val crossEdges =
                result.edges.filter { edge ->
                    val sourceCommunity = result.nodes.find { it.id == edge.source }?.community
                    val targetCommunity = result.nodes.find { it.id == edge.target }?.community
                    sourceCommunity != targetCommunity
                }
            // At least one cross-community edge exists
            assertThat(crossEdges).isNotEmpty
        }
    }

    // ──────────────────────────────────────
    // Edges with one endpoint outside the subgraph
    // ──────────────────────────────────────

    @Nested
    @DisplayName("SubgraphExtractor — orphan edges")
    inner class OrphanEdges {
        @Test
        @DisplayName("edges with one endpoint outside the subgraph are excluded")
        fun `edges with one endpoint outside subgraph are excluded`() {
            val config =
                LensConfig(
                    communities = listOf("bakery-gradle"),
                    nodeTypes = listOf("file"),
                    edgeTypes = listOf("reference", "agent_reference"),
                )
            val result = extractor.extract(testGraphModel, config)

            // All edges must have both endpoints inside the subgraph
            val nodeIds = result.nodeIds
            result.edges.forEach { edge ->
                assertThat(edge.source).isIn(nodeIds)
                assertThat(edge.target).isIn(nodeIds)
            }
        }
    }
}