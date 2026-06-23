package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

class ChunkValidatorTest {

    private val validator = ChunkValidator()

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun assertHasError(
        result: ValidationResult,
        errorType: ChunkValidationErrorType,
        messageFragment: String
    ) {
        assertTrue(
            result.errors.any { it.errorType == errorType && it.message.contains(messageFragment, ignoreCase = true) },
            "Expected error of type $errorType containing '$messageFragment', got ${result.errors}"
        )
    }

    private fun validChunk(
        content: String = "**INTERDICTION FORMELLE** de committer sans permission",
        checksum: String = sha256(content),
        id: String = sha256("AGENT.adoc:1-3:$content")
    ): AgenticChunk = AgenticChunk(
        id = id,
        sourceFile = "AGENT.adoc",
        sourceLines = "1-3",
        chunkType = ChunkType.RULE,
        content = content,
        verb = TaxonomyVerb.INTERDIRE,
        domain = "codebase",
        dagLevel = DagLevel.N1,
        circle = 4,
        weight = 1.0,
        checksum = checksum
    )

    @Test
    fun `should validate a well-formed chunk as valid`() {
        val chunk = validChunk()

        val result = validator.validate(chunk)

        assertTrue(result.valid, "Well-formed chunk should be valid")
        assertTrue(result.errors.isEmpty(), "Valid chunk should have no errors")
    }

    @Test
    fun `should reject chunk with blank id`() {
        val chunk = validChunk().copy(id = "")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with blank id should be invalid")
        assertHasError(result, ChunkValidationErrorType.MISSING_ID, "id")
    }

    @Test
    fun `should reject chunk with blank sourceFile`() {
        val chunk = validChunk().copy(sourceFile = "")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with blank sourceFile should be invalid")
        assertHasError(result, ChunkValidationErrorType.MISSING_SOURCE_FILE, "sourceFile")
    }

    @Test
    fun `should reject chunk with blank content`() {
        val chunk = validChunk().copy(content = "")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with blank content should be invalid")
        assertHasError(result, ChunkValidationErrorType.MISSING_CONTENT, "content")
    }

    @Test
    fun `should reject chunk with blank sourceLines`() {
        val chunk = validChunk().copy(sourceLines = "")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with blank sourceLines should be invalid")
        assertHasError(result, ChunkValidationErrorType.MISSING_SOURCE_LINES, "sourceLines")
    }

    @Test
    fun `should reject chunk with blank checksum`() {
        val chunk = validChunk().copy(checksum = "")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with blank checksum should be invalid")
        assertHasError(result, ChunkValidationErrorType.MISSING_CHECKSUM, "checksum")
    }

    @Test
    fun `should reject chunk with checksum not matching content`() {
        val content = "**INTERDICTION** de committer"
        val chunk = validChunk(content = content, checksum = "deadbeef")

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with mismatched checksum should be invalid")
        assertHasError(result, ChunkValidationErrorType.CHECKSUM_MISMATCH, "checksum")
    }

    @Test
    fun `should reject chunk with negative weight`() {
        val chunk = validChunk().copy(weight = -0.1)

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with negative weight should be invalid")
        assertHasError(result, ChunkValidationErrorType.NEGATIVE_WEIGHT, "weight")
    }

    @Test
    fun `should reject chunk with weight greater than one`() {
        val chunk = validChunk().copy(weight = 1.5)

        val result = validator.validate(chunk)

        assertTrue(!result.valid, "Chunk with weight > 1.0 should be invalid")
        assertHasError(result, ChunkValidationErrorType.WEIGHT_EXCEEDS_ONE, "weight")
    }

    @Test
    fun `should expose line range from sourceLines`() {
        val chunk = validChunk().copy(sourceLines = "12-15", id = "")

        val result = validator.validate(chunk)
        val error = result.errors.first { it.errorType == ChunkValidationErrorType.MISSING_ID }

        assertEquals(12, error.lineStart)
        assertEquals(15, error.lineEnd)
    }

    @Test
    fun `should expose single line range when sourceLines has no dash`() {
        val chunk = validChunk().copy(sourceLines = "7", id = "")

        val result = validator.validate(chunk)
        val error = result.errors.first { it.errorType == ChunkValidationErrorType.MISSING_ID }

        assertEquals(7, error.lineStart)
        assertEquals(7, error.lineEnd)
    }

    @Test
    fun `should expose null line range for blank sourceLines`() {
        val chunk = validChunk().copy(sourceLines = "", id = "")

        val result = validator.validate(chunk)
        val idError = result.errors.first { it.errorType == ChunkValidationErrorType.MISSING_ID }
        val linesError = result.errors.first { it.errorType == ChunkValidationErrorType.MISSING_SOURCE_LINES }

        assertEquals(null, idError.lineStart)
        assertEquals(null, idError.lineEnd)
        assertEquals(null, linesError.lineStart)
        assertEquals(null, linesError.lineEnd)
    }

    @Test
    fun `should accept chunk with weight at boundaries zero and one`() {
        val zero = validChunk().copy(weight = 0.0)
        val one = validChunk().copy(weight = 1.0)

        assertTrue(validator.validate(zero).valid, "Weight 0.0 should be valid")
        assertTrue(validator.validate(one).valid, "Weight 1.0 should be valid")
    }
}