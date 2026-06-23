package codebase.koog.agentic

import java.time.Instant

/**
 * Implémentation en mémoire du port [InvalidChunkRepository].
 * Utilisée pour les tests et le fallback quand aucune base pgvector n'est disponible.
 */
class InMemoryInvalidChunkRepository : InvalidChunkRepository {

    private val quarantined = mutableListOf<InvalidChunk>()

    override suspend fun insertQuarantine(chunk: AgenticChunk, errors: List<ChunkValidationError>): Boolean {
        quarantined.add(
            InvalidChunk(
                id = chunk.id,
                sourceFile = chunk.sourceFile,
                sourceLines = chunk.sourceLines,
                content = chunk.content,
                errors = errors,
                quarantinedAt = Instant.now()
            )
        )
        return true
    }

    override suspend fun listQuarantined(limit: Int): List<InvalidChunk> =
        quarantined.takeLast(limit)

    override suspend fun countQuarantined(): Int = quarantined.size
}
