package codebase.koog.agentic

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryInvalidChunkRepositoryTest {

    private val repository = InMemoryInvalidChunkRepository()

    @Test
    fun `insertQuarantine stores invalid chunk with errors`() {
        val chunk = AgenticChunk(
            id = "chunk-1",
            sourceFile = "AGENT.adoc",
            sourceLines = "10-15",
            chunkType = ChunkType.RULE,
            content = "bad content",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 0.8,
            checksum = "bad-checksum"
        )
        val errors = listOf(
            ChunkValidationError(
                sourceFile = "AGENT.adoc",
                sourceLines = "10-15",
                lineStart = 10,
                lineEnd = 15,
                errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                message = "checksum does not match content"
            )
        )

        val inserted = runBlocking { repository.insertQuarantine(chunk, errors) }

        assertTrue(inserted)
        assertEquals(1, runBlocking { repository.countQuarantined() })
        val quarantined = runBlocking { repository.listQuarantined(10) }.single()
        assertEquals("chunk-1", quarantined.id)
        assertEquals("AGENT.adoc", quarantined.sourceFile)
        assertEquals("10-15", quarantined.sourceLines)
        assertEquals("bad content", quarantined.content)
        assertEquals(1, quarantined.errors.size)
        assertEquals(ChunkValidationErrorType.CHECKSUM_MISMATCH, quarantined.errors.single().errorType)
        assertTrue(quarantined.quarantinedAt.isAfter(Instant.EPOCH))
    }

    @Test
    fun `listQuarantined respects limit`() {
        val chunk = AgenticChunk(
            id = "chunk-1",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-2",
            chunkType = ChunkType.RULE,
            content = "x",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "x-checksum"
        )
        val errors = listOf(
            ChunkValidationError(
                sourceFile = "AGENT.adoc",
                sourceLines = "1-2",
                lineStart = 1,
                lineEnd = 2,
                errorType = ChunkValidationErrorType.MISSING_ID,
                message = "id missing"
            )
        )

        repeat(3) { index ->
            runBlocking {
                repository.insertQuarantine(
                    chunk.copy(id = "chunk-$index", content = "x$index", checksum = "cs$index"),
                    errors
                )
            }
        }

        assertEquals(3, runBlocking { repository.countQuarantined() })
        assertEquals(2, runBlocking { repository.listQuarantined(2).size })
    }

    @Test
    fun `countQuarantined returns zero when empty`() {
        assertEquals(0, runBlocking { repository.countQuarantined() })
    }
}
