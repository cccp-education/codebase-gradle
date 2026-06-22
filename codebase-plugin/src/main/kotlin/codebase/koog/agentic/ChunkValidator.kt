package codebase.koog.agentic

import java.security.MessageDigest

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>
)

open class ChunkValidator {

    open fun validate(chunk: AgenticChunk): ValidationResult {
        val errors = mutableListOf<String>()

        if (chunk.id.isBlank()) errors.add("id must not be blank")
        if (chunk.sourceFile.isBlank()) errors.add("sourceFile must not be blank")
        if (chunk.content.isBlank()) errors.add("content must not be blank")
        if (chunk.sourceLines.isBlank()) errors.add("sourceLines must not be blank")
        if (chunk.checksum.isBlank()) {
            errors.add("checksum must not be blank")
        } else if (chunk.content.isNotBlank() && chunk.checksum != sha256(chunk.content)) {
            errors.add("checksum does not match content")
        }
        if (chunk.weight < 0.0) errors.add("weight must not be negative")
        if (chunk.weight > 1.0) errors.add("weight must not exceed 1.0")

        return ValidationResult(valid = errors.isEmpty(), errors = errors)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}