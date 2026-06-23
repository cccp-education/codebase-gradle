package codebase.koog.agentic

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvalidChunkQuarantineTest {

    @Test
    fun `quarantineIfInvalid stores invalid chunk`() {
        val repository = InMemoryInvalidChunkRepository()
        val quarantine = InvalidChunkQuarantine(repository)

        val chunk = AgenticChunk(
            id = "bad",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-2",
            chunkType = ChunkType.RULE,
            content = "x",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = -1.0,
            checksum = "x-checksum"
        )
        val errors = listOf(
            ChunkValidationError(
                sourceFile = "AGENT.adoc",
                sourceLines = "1-2",
                lineStart = 1,
                lineEnd = 2,
                errorType = ChunkValidationErrorType.NEGATIVE_WEIGHT,
                message = "weight must not be negative"
            )
        )

        val stored = runBlocking { quarantine.quarantineIfInvalid(chunk, ValidationResult(valid = false, errors = errors)) }

        assertTrue(stored)
        assertEquals(1, runBlocking { quarantine.countQuarantined() })
    }

    @Test
    fun `quarantineIfInvalid ignores valid chunk`() {
        val repository = InMemoryInvalidChunkRepository()
        val quarantine = InvalidChunkQuarantine(repository)

        val chunk = AgenticChunk(
            id = "ok",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-2",
            chunkType = ChunkType.RULE,
            content = "valid content",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 0.8,
            checksum = sha256("valid content")
        )

        val stored = runBlocking { quarantine.quarantineIfInvalid(chunk, ValidationResult(valid = true, errors = emptyList())) }

        assertFalse(stored)
        assertEquals(0, runBlocking { quarantine.countQuarantined() })
    }

    @Test
    fun `quarantineChunk validates and stores when invalid`() {
        val repository = InMemoryInvalidChunkRepository()
        val quarantine = InvalidChunkQuarantine(repository)

        val chunk = AgenticChunk(
            id = "",
            sourceFile = "",
            sourceLines = "",
            chunkType = ChunkType.RULE,
            content = "",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = -1.0,
            checksum = ""
        )

        val stored = runBlocking { quarantine.quarantineChunk(chunk) }

        assertTrue(stored)
        assertEquals(1, runBlocking { quarantine.countQuarantined() })
    }

    @Test
    fun `quarantineChunk does not store valid chunk`() {
        val repository = InMemoryInvalidChunkRepository()
        val quarantine = InvalidChunkQuarantine(repository)

        val chunk = AgenticChunk(
            id = "ok",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-2",
            chunkType = ChunkType.RULE,
            content = "valid content",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 0.8,
            checksum = sha256("valid content")
        )

        val stored = runBlocking { quarantine.quarantineChunk(chunk) }

        assertFalse(stored)
        assertEquals(0, runBlocking { quarantine.countQuarantined() })
    }

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
