package codebase.koog.agentic

/**
 * Service DDD responsable de la mise en quarantaine des chunks invalides.
 *
 * Découple la validation (ChunkValidator) du stockage (InvalidChunkRepository)
 * et fournit un point d'entrée unique pour l'AgenticIngestor.
 */
class InvalidChunkQuarantine(
    private val repository: InvalidChunkRepository
) {

    suspend fun quarantineIfInvalid(chunk: AgenticChunk, validationResult: ValidationResult): Boolean {
        if (validationResult.valid) return false
        return repository.insertQuarantine(chunk, validationResult.errors)
    }

    suspend fun quarantineChunk(chunk: AgenticChunk, validator: ChunkValidator = ChunkValidator()): Boolean {
        val result = validator.validate(chunk)
        return quarantineIfInvalid(chunk, result)
    }

    suspend fun listQuarantined(limit: Int = 100): List<InvalidChunk> =
        repository.listQuarantined(limit)

    suspend fun countQuarantined(): Int =
        repository.countQuarantined()
}
