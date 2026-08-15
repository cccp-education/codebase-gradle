package codebase.graph

/**
 * Lens configuration — knowledge graph segregation (EPIC SUBGRAPH US-1).
 *
 * Controls the filtering of the global graph (203k nodes) into a targeted
 * subgraph (~200 nodes) for injection into the augmented context.
 *
 * Ported from bakery LensConfig (BKY-LENS-1) into the shared N1 socle.
 * The enrichment concerns (RAG pgvector, editorial rules) stay in bakery
 * (LENS-2) — this domain only covers segregation.
 *
 * Layers of the LENS pattern:
 * 1. SEGREGATION (this config): filter by communities, types, edges, depth
 * 2. ENRICHMENT (bakery LENS-2): RAG pgvector + editorial rules
 * 3. BUDGET (bakery LENS-3): truncate to N articles/page, min similarity
 *
 * Usage:
 * ```
 * val config = LensConfig(
 *     scope = LensScope.SUBGRAPH,
 *     communities = listOf("bakery-gradle", "codebase-gradle"),
 *     nodeTypes = listOf("file"),
 *     edgeTypes = listOf("reference", "agent_reference"),
 *     maxDepth = 2,
 *     fileExtensions = listOf("adoc", "md", "html"),
 * )
 * ```
 */
data class LensConfig(
    /** Lens scope: SUBGRAPH (filtered), FULL (whole graph), SEMANTIC_ONLY (RAG only) */
    var scope: LensScope = LensScope.SUBGRAPH,
    /** Communities to include in the subgraph (ex: ["bakery-gradle", "codebase-gradle"]) */
    var communities: List<String> = emptyList(),
    /** Node types to include (ex: ["file", "module", "class"]) */
    var nodeTypes: List<String> = listOf("file"),
    /** Edge types to include (ex: ["reference", "agent_reference"]) */
    var edgeTypes: List<String> = listOf("reference", "agent_reference"),
    /** Maximum neighborhood depth from the current page (default: 2) */
    var maxDepth: Int = 2,
    /** File extensions to keep (ex: ["adoc", "md", "html"]) */
    var fileExtensions: List<String> = listOf("adoc", "md", "html"),
)

/**
 * Lens scope.
 * - SUBGRAPH: filters the graph by communities/types/edges/depth
 * - FULL: uses the whole graph (for debug or small workspaces)
 * - SEMANTIC_ONLY: ignores the graph, uses RAG only
 */
enum class LensScope {
    SUBGRAPH,
    FULL,
    SEMANTIC_ONLY,
}