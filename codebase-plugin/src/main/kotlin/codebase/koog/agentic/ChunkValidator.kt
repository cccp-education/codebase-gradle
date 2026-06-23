package codebase.koog.agentic

import java.security.MessageDigest

enum class ChunkValidationErrorType {
    MISSING_ID,
    MISSING_SOURCE_FILE,
    MISSING_CONTENT,
    MISSING_SOURCE_LINES,
    MISSING_CHECKSUM,
    CHECKSUM_MISMATCH,
    NEGATIVE_WEIGHT,
    WEIGHT_EXCEEDS_ONE
}

data class ChunkValidationError(
    val sourceFile: String,
    val sourceLines: String,
    val lineStart: Int?,
    val lineEnd: Int?,
    val errorType: ChunkValidationErrorType,
    val message: String
)

data class ValidationResult(
    val valid: Boolean,
    val errors: List<ChunkValidationError>
)

open class ChunkValidator {

    open fun validate(chunk: AgenticChunk): ValidationResult {
        val errors = mutableListOf<ChunkValidationError>()

        if (chunk.id.isBlank()) {
            errors.add(error(chunk, ChunkValidationErrorType.MISSING_ID, "id must not be blank"))
        }
        if (chunk.sourceFile.isBlank()) {
            errors.add(error(chunk, ChunkValidationErrorType.MISSING_SOURCE_FILE, "sourceFile must not be blank"))
        }
        if (chunk.content.isBlank()) {
            errors.add(error(chunk, ChunkValidationErrorType.MISSING_CONTENT, "content must not be blank"))
        }
        if (chunk.sourceLines.isBlank()) {
            errors.add(error(chunk, ChunkValidationErrorType.MISSING_SOURCE_LINES, "sourceLines must not be blank"))
        }
        if (chunk.checksum.isBlank()) {
            errors.add(error(chunk, ChunkValidationErrorType.MISSING_CHECKSUM, "checksum must not be blank"))
        } else if (chunk.content.isNotBlank() && chunk.checksum != sha256(chunk.content)) {
            errors.add(error(chunk, ChunkValidationErrorType.CHECKSUM_MISMATCH, "checksum does not match content"))
        }
        if (chunk.weight < 0.0) {
            errors.add(error(chunk, ChunkValidationErrorType.NEGATIVE_WEIGHT, "weight must not be negative"))
        }
        if (chunk.weight > 1.0) {
            errors.add(error(chunk, ChunkValidationErrorType.WEIGHT_EXCEEDS_ONE, "weight must not exceed 1.0"))
        }

        return ValidationResult(valid = errors.isEmpty(), errors = errors)
    }

    private fun error(
        chunk: AgenticChunk,
        errorType: ChunkValidationErrorType,
        message: String
    ): ChunkValidationError {
        val (lineStart, lineEnd) = parseLineRange(chunk.sourceLines)
        return ChunkValidationError(
            sourceFile = chunk.sourceFile,
            sourceLines = chunk.sourceLines,
            lineStart = lineStart,
            lineEnd = lineEnd,
            errorType = errorType,
            message = message
        )
    }

    private fun parseLineRange(sourceLines: String): Pair<Int?, Int?> {
        if (sourceLines.isBlank()) return null to null
        val parts = sourceLines.split("-")
        val start = parts.firstOrNull()?.toIntOrNull()
        val end = if (parts.size > 1) parts.lastOrNull()?.toIntOrNull() else start
        return start to end
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}