package codebase.koog.agentic

enum class TaxonomySection {
    PRINCIPES,
    TAXONOMIE,
    FORMAT_PIVOT,
    CONVENTION_OVER_CONFIGURATION,
    CONFIG_DOMAINE,
    MAPPING,
    ROADMAP_IMPLEMENTATION,
    DEPENDANCES,
    ORDRE_ATTAQUE,
    EXEMPLES_STDOUT,
    CONCLUSION,
    UNKNOWN
}

data class OntologizedChunk(
    val chunk: AgenticChunk,
    val taxonomySection: TaxonomySection,
    val ontologyConfidence: Double,
    val relatedChunkIds: List<String>
)
