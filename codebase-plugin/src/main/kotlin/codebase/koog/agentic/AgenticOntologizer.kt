package codebase.koog.agentic

class AgenticOntologizer {

    fun ontologize(chunks: List<AgenticChunk>): List<OntologizedChunk> {
        if (chunks.isEmpty()) return emptyList()

        val ontologized = mutableListOf<OntologizedChunk>()

        for (chunk in chunks) {
            val section = mapToTaxonomySection(chunk)
            val confidence = computeOntologyConfidence(chunk, section)
            val relatedIds = findRelatedChunkIds(chunk, chunks)

            ontologized.add(
                OntologizedChunk(
                    chunk = chunk,
                    taxonomySection = section,
                    ontologyConfidence = confidence,
                    relatedChunkIds = relatedIds
                )
            )
        }

        return ontologized
    }

    private fun mapToTaxonomySection(chunk: AgenticChunk): TaxonomySection {
        val content = chunk.content.lowercase()

        return when {
            content.contains("principe") || content.contains("fondateur") ||
                content.contains("le verbe dit pourquoi") -> TaxonomySection.PRINCIPES

            content.contains("taxonomie") || content.contains("quatre verbes") ||
                content.contains("generer") && content.contains("collecter") &&
                content.contains("transformer") && content.contains("deployer") -> TaxonomySection.TAXONOMIE

            content.contains("format pivot") || content.contains("metadata.json") ||
                content.contains("contrat d'interface") -> TaxonomySection.FORMAT_PIVOT

            content.contains("convention over configuration") ||
                content.contains("inférence") || content.contains("magic value") ||
                content.contains("arborescence") -> TaxonomySection.CONVENTION_OVER_CONFIGURATION

            content.contains("configuration par domaine") ||
                content.contains("namespace") && content.contains("extension") -> TaxonomySection.CONFIG_DOMAINE

            content.contains("mapping") && content.contains("borough") ||
                content.contains("tâche actuelle") && content.contains("tâche renommée") -> TaxonomySection.MAPPING

            content.contains("roadmap") || content.contains("phase") && content.contains("k-") -> TaxonomySection.ROADMAP_IMPLEMENTATION

            content.contains("dépendance") || content.contains("epic g") ||
                content.contains("parallélisable") -> TaxonomySection.DEPENDANCES

            content.contains("ordre d'attaque") || content.contains("phase 0") ||
                content.contains("bootstrap artisanal") -> TaxonomySection.ORDRE_ATTAQUE

            content.contains("exemple") && content.contains("stdout") ||
                content.contains("avant/après") || content.contains("gradlew tasks") -> TaxonomySection.EXEMPLES_STDOUT

            content.contains("conclusion") || content.contains("ce qu'on importe") ||
                content.contains("ce qu'on n'importe pas") -> TaxonomySection.CONCLUSION

            else -> TaxonomySection.UNKNOWN
        }
    }

    private fun computeOntologyConfidence(chunk: AgenticChunk, section: TaxonomySection): Double {
        var score = 0.0

        if (section != TaxonomySection.UNKNOWN) score += 0.4

        if (chunk.verb != null) score += 0.2

        if (chunk.domain != null) score += 0.15

        if (chunk.dagLevel != null) score += 0.15

        if (chunk.circle != null) score += 0.1

        return score.coerceIn(0.0, 1.0)
    }

    private fun findRelatedChunkIds(chunk: AgenticChunk, allChunks: List<AgenticChunk>): List<String> {
        val related = mutableListOf<String>()

        for (other in allChunks) {
            if (other.id == chunk.id) continue

            var relationScore = 0

            if (chunk.domain != null && chunk.domain == other.domain) relationScore++
            if (chunk.verb != null && chunk.verb == other.verb) relationScore++
            if (chunk.dagLevel != null && chunk.dagLevel == other.dagLevel) relationScore++
            if (chunk.circle != null && chunk.circle == other.circle) relationScore++
            if (chunk.chunkType == other.chunkType) relationScore++

            if (relationScore >= 2) {
                related.add(other.id)
            }
        }

        return related
    }
}
