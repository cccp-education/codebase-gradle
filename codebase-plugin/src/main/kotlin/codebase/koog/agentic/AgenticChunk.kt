package codebase.koog.agentic

import java.security.MessageDigest

enum class ChunkType {
    RULE,
    CONCEPT,
    PROCEDURE,
    METADATA,
    CONSTRAINT
}

enum class TaxonomyVerb {
    GENERER,
    COLLECTER,
    TRANSFORMER,
    DEPLOYER,
    INTERDIRE,
    VALIDER
}

enum class DagLevel(val level: Int) {
    N0(0),
    N1(1),
    N2(2),
    N3(3),
    N4(4);

    companion object {
        fun fromString(s: String): DagLevel? = entries.firstOrNull {
            it.name.equals(s.trim(), ignoreCase = true)
        }
    }
}

data class AgenticChunk(
    val id: String,
    val sourceFile: String,
    val sourceLines: String,
    val chunkType: ChunkType,
    val content: String,
    val verb: TaxonomyVerb?,
    val domain: String?,
    val dagLevel: DagLevel?,
    val circle: Int?,
    val weight: Double,
    val checksum: String
)
