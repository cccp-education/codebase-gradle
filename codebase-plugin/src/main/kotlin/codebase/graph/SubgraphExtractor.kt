package codebase.graph

// WORKAROUND: graphify-plugin 0.0.2 is published with relocated package com.cheroliv.graphify.model
// instead of graphify.model. Revert to graphify.model.* once graphify-plugin 0.0.3 is fixed.
import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Subgraph extractor — LENS pattern segregation (EPIC SUBGRAPH US-1).
 *
 * Filters the global graph (203k nodes, 188k edges) into a targeted
 * subgraph (~200 nodes) according to the [LensConfig] criteria:
 * - included communities
 * - included node types
 * - included edge types
 * - maximum neighborhood depth
 * - file extensions
 *
 * The result is a [SiteSubgraph] ready for injection into the augmented
 * context (ContextChannel.Graphify).
 *
 * Ported from bakery SubgraphExtractor (BKY-LENS-1) into the shared N1
 * socle codebase.graph. Bakery must later consume codebase instead of
 * duplicating this extractor.
 *
 * Architecture:
 * ```
 * graph.json (203k nodes)
 *     ↓ SubgraphExtractor.extract(graphModel, lensConfig)
 * site-subgraph (~200 nodes, ~300 edges)
 *     ↓ + RAG pgvector + business rules (bakery LENS-2)
 * site-graph ready for injection
 * ```
 *
 * DAG contract: codebase (N1) imports graphify-plugin (N0).
 */
class SubgraphExtractor {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Loads a graph.json file and returns the [GraphModel].
     */
    fun loadGraph(graphFilePath: String): GraphModel {
        val file = File(graphFilePath)
        if (!file.exists()) {
            return GraphModel(nodes = emptyList(), edges = emptyList(), communities = emptyList())
        }
        return objectMapper.readValue<GraphModel>(file)
    }

    /**
     * Extracts a subgraph from the global graph according to [LensConfig].
     *
     * Steps:
     * 1. Filter nodes by communities + types + extensions
     * 2. Filter edges by types
     * 3. Keep edges whose both endpoints are in the subgraph
     * 4. Apply the max depth if a seed set is specified
     *
     * @param graphModel The complete graph loaded from graph.json
     * @param lensConfig The lens configuration
     * @return [SiteSubgraph] containing the filtered nodes and edges
     */
    fun extract(
        graphModel: GraphModel,
        lensConfig: LensConfig,
    ): SiteSubgraph {
        if (lensConfig.scope == LensScope.FULL) {
            return SiteSubgraph(
                nodes = graphModel.nodes,
                edges = graphModel.edges,
                communities = graphModel.communities,
            )
        }

        if (lensConfig.scope == LensScope.SEMANTIC_ONLY) {
            return SiteSubgraph(
                nodes = emptyList(),
                edges = emptyList(),
                communities = emptyList(),
            )
        }

        // SUBGRAPH — filter by communities, types, extensions

        // 1. Filter nodes
        val filteredNodes =
            graphModel.nodes.filter { node ->
                val communityMatch =
                    lensConfig.communities.isEmpty() ||
                        node.community in lensConfig.communities ||
                        node.community == null // nodes without community = orphans

                val typeMatch =
                    lensConfig.nodeTypes.isEmpty() ||
                        node.type in lensConfig.nodeTypes

                val extensionMatch =
                    lensConfig.fileExtensions.isEmpty() ||
                        node.id.substringAfterLast('.', "").lowercase() in lensConfig.fileExtensions ||
                        node.type != "file" // modules always pass the extension filter

                communityMatch && typeMatch && extensionMatch
            }

        // 2. Filter edges by type
        val edgeTypeSet = lensConfig.edgeTypes.toSet()
        val filteredEdges =
            graphModel.edges.filter { edge ->
                edgeTypeSet.isEmpty() || edge.type in edgeTypeSet
            }

        // 3. Keep edges whose both endpoints are in the subgraph
        val nodeIds = filteredNodes.map { it.id }.toSet()
        val connectedEdges =
            filteredEdges.filter { edge ->
                edge.source in nodeIds && edge.target in nodeIds
            }

        // 4. Apply the max depth
        val resultNodes =
            if (lensConfig.communities.isNotEmpty()) {
                // BFS from the nodes of the targeted communities
                // maxDepth=0 → seeds only, maxDepth=1 → +direct neighbors, etc.
                val seedIds =
                    filteredNodes
                        .filter { it.community in lensConfig.communities }
                        .map { it.id }
                        .toSet()
                expandBfs(seedIds, nodeIds, connectedEdges, lensConfig.maxDepth)
            } else {
                // No target community → all filtered nodes
                nodeIds
            }

        val finalNodes = filteredNodes.filter { it.id in resultNodes }
        val finalEdges = connectedEdges.filter { it.source in resultNodes && it.target in resultNodes }

        // Filter visible communities
        val visibleCommunityIds = finalNodes.mapNotNull { it.community }.toSet()
        val visibleCommunities = graphModel.communities.filter { it.id in visibleCommunityIds }

        return SiteSubgraph(
            nodes = finalNodes,
            edges = finalEdges,
            communities = visibleCommunities,
        )
    }

    /**
     * Extracts a subgraph from a graph.json file.
     * Convenience method combining [loadGraph] and [extract].
     */
    fun extractFromPath(
        graphFilePath: String,
        lensConfig: LensConfig,
    ): SiteSubgraph {
        val graphModel = loadGraph(graphFilePath)
        return extract(graphModel, lensConfig)
    }

    /**
     * BFS expansion from seed nodes.
     * Returns the set of node IDs reachable in at most [maxDepth] hops.
     * maxDepth=0 returns the seeds only.
     */
    private fun expandBfs(
        seedIds: Set<String>,
        candidateIds: Set<String>,
        edges: List<GraphEdge>,
        maxDepth: Int,
    ): Set<String> {
        val seeds = seedIds.intersect(candidateIds)
        if (maxDepth <= 0) return seeds

        // Build the (bidirectional) adjacency
        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (edge in edges) {
            if (edge.source in candidateIds && edge.target in candidateIds) {
                adjacency.getOrPut(edge.source) { mutableListOf() }.add(edge.target)
                adjacency.getOrPut(edge.target) { mutableListOf() }.add(edge.source)
            }
        }

        val visited = mutableSetOf<String>()
        visited.addAll(seeds)

        var currentLevel = seeds.toMutableSet()

        for (depth in 1..maxDepth) {
            if (currentLevel.isEmpty()) break

            val nextLevel = mutableSetOf<String>()
            for (nodeId in currentLevel) {
                for (neighbor in adjacency[nodeId] ?: emptyList()) {
                    if (neighbor !in visited && neighbor in candidateIds) {
                        nextLevel.add(neighbor)
                    }
                }
            }
            visited.addAll(nextLevel)
            currentLevel = nextLevel.toMutableSet()
        }

        return visited
    }
}

/**
 * Result of the graph filtering — targeted subgraph.
 *
 * Contains the filtered nodes, edges and communities ready for
 * injection into the augmented context (ContextChannel.Graphify).
 */
data class SiteSubgraph(
    /** Filtered nodes of the subgraph */
    val nodes: List<GraphNode>,
    /** Filtered edges (both endpoints in [nodes]) */
    val edges: List<GraphEdge>,
    /** Communities visible in the subgraph */
    val communities: List<GraphCommunity>,
) {
    /** Number of nodes in the subgraph */
    val nodeCount: Int get() = nodes.size

    /** Number of edges in the subgraph */
    val edgeCount: Int get() = edges.size

    /** Number of visible communities */
    val communityCount: Int get() = communities.size

    /** Node IDs for fast lookup */
    val nodeIds: Set<String> get() = nodes.map { it.id }.toSet()

    /** IDs of the visible communities */
    val communityIds: Set<String> get() = communities.map { it.id }.toSet()

    /** Nodes grouped by community */
    val nodesByCommunity: Map<String?, List<GraphNode>> get() = nodes.groupBy { it.community }

    /** Edges grouped by type */
    val edgesByType: Map<String, List<GraphEdge>> get() = edges.groupBy { it.type }

    /** Returns the nodes of a given community */
    fun nodesInCommunity(communityId: String): List<GraphNode> = nodes.filter { it.community == communityId }

    /** Returns the edges of a given type */
    fun edgesOfType(type: String): List<GraphEdge> = edges.filter { it.type == type }

    /** Returns the neighbors of a node */
    fun neighbors(nodeId: String): List<GraphNode> {
        val neighborIds =
            edges
                .filter { it.source == nodeId || it.target == nodeId }
                .map { if (it.source == nodeId) it.target else it.source }
                .toSet()
        return nodes.filter { it.id in neighborIds }
    }
}