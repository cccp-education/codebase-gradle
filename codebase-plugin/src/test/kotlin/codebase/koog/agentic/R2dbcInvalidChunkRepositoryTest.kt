package codebase.koog.agentic

import codebase.infrastructure.PostgresFixture
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.reactive.awaitSingle
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class R2dbcInvalidChunkRepositoryTest {

    companion object {
        private lateinit var connectionFactory: PostgresqlConnectionFactory

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = PostgresqlConnectionConfiguration.builder()
                .host(PostgresFixture.host)
                .port(PostgresFixture.port)
                .database(PostgresFixture.databaseName)
                .username(PostgresFixture.username)
                .password(PostgresFixture.password)
                .build()
            connectionFactory = PostgresqlConnectionFactory(config)
            runBlocking {
                val conn = Mono.from(connectionFactory.create()).awaitSingle()
                try {
                    Mono.from(
                        conn.createStatement("DELETE FROM invalid_chunks").execute()
                    ).subscribe()
                } finally {
                    Mono.from(conn.close()).subscribe()
                }
            }
        }
    }

    @Test
    fun `quarantine and list invalid chunks in PostgreSQL`() {
        val repository = R2dbcInvalidChunkRepository(connectionFactory)
        runBlocking { repository.initSchema() }

        val chunkId = java.util.UUID.randomUUID().toString()
        val chunk = AgenticChunk(
            id = chunkId,
            sourceFile = "AGENT.adoc",
            sourceLines = "5-8",
            chunkType = ChunkType.RULE,
            content = "bad content",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 0.9,
            checksum = "bad-checksum"
        )
        val errors = listOf(
            ChunkValidationError(
                sourceFile = "AGENT.adoc",
                sourceLines = "5-8",
                lineStart = 5,
                lineEnd = 8,
                errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                message = "checksum does not match content"
            )
        )

        val inserted = runBlocking { repository.insertQuarantine(chunk, errors) }
        assertTrue(inserted, "Chunk should be inserted")

        val count = runBlocking { repository.countQuarantined() }
        assertTrue(count >= 1, "At least one quarantined chunk should exist")

        val quarantined = runBlocking { repository.listQuarantined(10) }
        val found = quarantined.find { it.id == chunkId }
        assertTrue(found != null, "Quarantined chunk should be retrievable")
        assertEquals("AGENT.adoc", found.sourceFile)
        assertEquals("bad content", found.content)
        assertEquals(1, found.errors.size)
        assertEquals(ChunkValidationErrorType.CHECKSUM_MISMATCH, found.errors.single().errorType)
    }

    @Test
    fun `inserting same id twice is idempotent`() {
        val repository = R2dbcInvalidChunkRepository(connectionFactory)
        runBlocking { repository.initSchema() }

        val chunkId = java.util.UUID.randomUUID().toString()
        val chunk = AgenticChunk(
            id = chunkId,
            sourceFile = "AGENT.adoc",
            sourceLines = "1-2",
            chunkType = ChunkType.RULE,
            content = "x",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "cs"
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

        val first = runBlocking { repository.insertQuarantine(chunk, errors) }
        val second = runBlocking { repository.insertQuarantine(chunk, errors) }

        assertTrue(first)
        assertEquals(false, second)
    }
}
