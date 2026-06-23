package codebase.koog.agentic

import java.time.Instant

/**
 * Représentation d'un chunk invalide mis en quarantaine.
 */
data class InvalidChunk(
    val id: String,
    val sourceFile: String,
    val sourceLines: String,
    val content: String,
    val errors: List<ChunkValidationError>,
    val quarantinedAt: Instant
)

/**
 * Port DDD de stockage des chunks invalides.
 */
interface InvalidChunkRepository {

    suspend fun insertQuarantine(chunk: AgenticChunk, errors: List<ChunkValidationError>): Boolean

    suspend fun listQuarantined(limit: Int = 100): List<InvalidChunk>

    suspend fun countQuarantined(): Int

    suspend fun initSchema() {}
}
