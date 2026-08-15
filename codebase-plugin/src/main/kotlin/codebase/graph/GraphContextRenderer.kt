package codebase.graph

/**
 * Renders a [SiteSubgraph] into the structured text injected into the
 * Graphify context channel (EPIC SUBGRAPH US-2).
 *
 * Pure object — no I/O, no Gradle, unit-testable without any fixture.
 * The output is line-based so the existing token-budget truncation
 * (truncateTokens / truncateToTokens) applies cleanly: the header line
 * is always kept, then nodes, edges and communities.
 *
 * Format (one line per entry):
 * ```
 * [Graphify] subgraph: 5 nodes, 4 edges, 2 communities
 * - node <id> [type=<type>, community=<community>]
 * - edge <source> -> <target> [type=<edgeType>]
 * - community <id> (<N> nodes)
 * ```
 */
object GraphContextRenderer {

    fun render(subgraph: SiteSubgraph): String = buildString {
        appendLine("[Graphify] subgraph: ${subgraph.nodeCount} nodes, ${subgraph.edgeCount} edges, ${subgraph.communityCount} communities")
        for (node in subgraph.nodes) {
            appendLine("- node ${node.id} [type=${node.type}, community=${node.community}]")
        }
        for (edge in subgraph.edges) {
            appendLine("- edge ${edge.source} -> ${edge.target} [type=${edge.type}]")
        }
        for (community in subgraph.communities) {
            appendLine("- community ${community.id} (${community.size} nodes)")
        }
    }.trimEnd()
}