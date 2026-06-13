package codebase.koog.agentic

class AgenticCompiler {

    fun compile(chunk: OntologizedChunk): CompiledArtifact? {
        val c = chunk.chunk
        val artifactType = determineArtifactType(c.chunkType, c.verb)
        val description = buildDescription(c.content, c.chunkType, c.verb)
        val confidence = computeConfidence(chunk)

        return CompiledArtifact(
            sourceChunkId = c.id,
            artifactType = artifactType,
            description = description,
            targetHint = c.domain,
            confidence = confidence
        )
    }

    private fun determineArtifactType(chunkType: ChunkType, verb: TaxonomyVerb?): ArtifactType {
        return when (chunkType) {
            ChunkType.RULE -> when (verb) {
                TaxonomyVerb.INTERDIRE -> ArtifactType.PRE_HOOK
                else -> ArtifactType.CI_GATE
            }
            ChunkType.PROCEDURE -> when (verb) {
                TaxonomyVerb.GENERER, TaxonomyVerb.DEPLOYER,
                TaxonomyVerb.TRANSFORMER, TaxonomyVerb.COLLECTER -> ArtifactType.GRADLE_TASK
                TaxonomyVerb.VALIDER -> ArtifactType.VALIDATION
                else -> ArtifactType.VALIDATION
            }
            ChunkType.CONSTRAINT -> ArtifactType.CONSTRAINT_CHECK
            ChunkType.CONCEPT -> when (verb) {
                TaxonomyVerb.INTERDIRE -> ArtifactType.PROMPT_TEMPLATE
                else -> ArtifactType.METADATA
            }
            ChunkType.METADATA -> ArtifactType.METADATA
        }
    }

    private fun buildDescription(content: String, chunkType: ChunkType, verb: TaxonomyVerb?): String {
        val lines = content.lines()
        val firstLine = lines.firstOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("=") } ?: content
        val prefix = when (chunkType) {
            ChunkType.RULE -> "Rule"
            ChunkType.PROCEDURE -> "Procedure"
            ChunkType.CONSTRAINT -> "Constraint"
            ChunkType.CONCEPT -> "Concept"
            ChunkType.METADATA -> "Metadata"
        }
        val verbSuffix = if (verb != null) " (${verb.name})" else ""
        return "$prefix$verbSuffix: ${firstLine.trim().take(120)}"
    }

    private fun computeConfidence(chunk: OntologizedChunk): Double {
        val base = chunk.ontologyConfidence * 0.6 + chunk.chunk.weight * 0.4
        return base.coerceIn(0.0, 1.0)
    }
}
