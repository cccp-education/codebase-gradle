package codebase.store

import codebase.infrastructure.PostgresFixture
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import java.util.UUID
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * EPIC CB-PAGE-PROVENANCE US-1 — functional testcontainers proof (D2-D5) :
 * `pages` persist at ingestion (`TEXT` comma-joined) and ride the
 * retrieval, exact (no TOC heuristic). Round-trip ingestion → retrieval
 * on the real [RagVectorStore] / pgvector.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RagVectorStorePageProvenanceTest {

    private val sourceDocument = "page-provenance-test-${UUID.randomUUID()}"

    private val store = RagVectorStore(
        host = PostgresFixture.host,
        port = PostgresFixture.port,
        database = PostgresFixture.databaseName,
        username = PostgresFixture.username,
        password = PostgresFixture.password
    )

    private val chunkWithPages = DocumentChunk(
        id = "chk-pages-01",
        sourceDocument = sourceDocument,
        sectionPath = "Chapter 1",
        headingLevel = 1,
        content = "referentiel competences de la formation professionnelle",
        pages = listOf(40, 41)
    )

    private val chunkWithoutPages = DocumentChunk(
        id = "chk-pages-02",
        sourceDocument = sourceDocument,
        sectionPath = "Chapter 2",
        headingLevel = 1,
        content = "references pedagogiques du programme de formation"
    )

    @BeforeAll
    fun setup() {
        // Empty ingest runs initSchema + doubtSchema + provenanceSchema —
        // idempotent, creates the tables for the cleanup DELETE below.
        runBlocking { store.ingestWithDoubt(emptyList()) }
        deleteTestRows()
    }

    @AfterAll
    fun teardown() {
        deleteTestRows()
    }

    @Test
    fun `pages round-trip from ingestion to retrieval`() {
        val ingested = runBlocking {
            store.ingestWithDoubt(
                listOf(
                    DoubtfulChunk(chunk = chunkWithPages, doubt = DoubtMetadata.fromConfidence(0.95)),
                    DoubtfulChunk(chunk = chunkWithoutPages, doubt = DoubtMetadata.fromConfidence(0.3))
                )
            )
        }
        assertEquals(1, ingested, "exactly one test document should be ingested")

        val results = runBlocking { store.searchWithDoubtBlocking("referentiel competences", topK = 10) }
        val own = results.filter { it.sourceDocument == sourceDocument }
        assertEquals(2, own.size, "both test chunks should be retrieved on this topic")

        val withPages = own.first { it.chunkIndex == 0 }
        assertEquals(listOf(40, 41), withPages.pages, "page provenance should survive the round-trip")

        val withoutPages = own.first { it.chunkIndex == 1 }
        assertTrue(withoutPages.pages.isEmpty(), "legacy/absent pages should parse to empty list")
        assertTrue(withoutPages.doubtful, "doubt of the weak chunk should also survive")
    }

    @Test
    fun `plain searchBlocking also reads pages`() {
        runBlocking { store.ingestWithDoubt(listOf(DoubtfulChunk(chunk = chunkWithPages))) }
        val results = runBlocking { store.searchBlocking("referentiel competences", topK = 10) }
        val found = results.firstOrNull { it.sourceDocument == sourceDocument }
        assertNotNull(found, "searchBlocking should retrieve the page-bearing chunk")
        assertEquals(listOf(40, 41), found.pages)
    }

    private fun deleteTestRows() {
        runBlocking {
            val factory = PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                    .host(PostgresFixture.host)
                    .port(PostgresFixture.port)
                    .database(PostgresFixture.databaseName)
                    .username(PostgresFixture.username)
                    .password(PostgresFixture.password)
                    .build()
            )
            val conn = factory.create().awaitFirst()
            try {
                // codex_chunks rows cascade via ON DELETE CASCADE.
                conn.createStatement("DELETE FROM codex_documents WHERE source_document = \$1")
                    .bind(0, sourceDocument)
                    .execute()
                    .awaitFirst()
            } finally {
                conn.close().awaitFirstOrNull()
            }
        }
    }
}