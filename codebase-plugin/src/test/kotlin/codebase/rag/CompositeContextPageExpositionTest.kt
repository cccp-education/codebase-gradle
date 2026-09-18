package codebase.rag

import codebase.infrastructure.PostgresFixture
import codebase.store.DocumentChunk
import codebase.store.DoubtMetadata
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * EPIC CB-PAGE-PROVENANCE US-3 (D8) — functional testcontainers proof of
 * the N1 exposition: pages persisted at ingestion (US-1) ride the
 * retrieval and reach the Docs channel of the composite context.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompositeContextPageExpositionTest {

    private val sourceDocument = "page-exposition-test-${UUID.randomUUID()}"
    private val marker = "traceable-${UUID.randomUUID()}"

    private val store = RagVectorStore(
        host = PostgresFixture.host,
        port = PostgresFixture.port,
        database = PostgresFixture.databaseName,
        username = PostgresFixture.username,
        password = PostgresFixture.password
    )

    private val chunkWithPages = DocumentChunk(
        id = "chk-expo-01",
        sourceDocument = sourceDocument,
        sectionPath = "Chapter 1",
        headingLevel = 1,
        content = "referentiel competences de la formation professionnelle $marker",
        pages = listOf(40, 41)
    )

    private val chunkWithoutPages = DocumentChunk(
        id = "chk-expo-02",
        sourceDocument = sourceDocument,
        sectionPath = "Chapter 2",
        headingLevel = 1,
        content = "references pedagogiques du programme de formation $marker"
    )

    @BeforeAll
    fun setup() {
        runBlocking { store.ingestWithDoubt(emptyList()) }
        deleteTestRows()
    }

    @AfterAll
    fun teardown() {
        deleteTestRows()
    }

    @Test
    fun `docs channel exposes pages from a real round-trip`() {
        val ingested = runBlocking {
            store.ingestWithDoubt(
                listOf(
                    DoubtfulChunk(chunk = chunkWithPages, doubt = DoubtMetadata.fromConfidence(0.95)),
                    DoubtfulChunk(chunk = chunkWithoutPages, doubt = DoubtMetadata.fromConfidence(0.3))
                )
            )
        }
        assertEquals(1, ingested, "exactly one test document should be ingested")

        val builder = CompositeContextBuilder(
            workspaceRoot = File(System.getProperty("java.io.tmpdir")),
            vectorStore = VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy"),
            embeddingPipeline = EmbeddingPipeline(
                VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy")
            ),
            config = contracts.context.CompositeContextConfig(),
            codexStore = store,
        )
        val composite = builder.build("$marker competences")
        val docs = composite.docsSection

        val ownLines = docs.lines().filter { it.startsWith("[Doc] source=$sourceDocument") }
        val pagedLines = ownLines.filter { it.contains("Chapter 1") }
        assertTrue(pagedLines.any { it.contains("pages=[40,41]") },
            "Docs channel should expose the page provenance of the paged chunk, got: $docs")

        val legacyLines = ownLines.filter { it.contains("Chapter 2") }
        assertTrue(legacyLines.any { !it.contains("pages=") },
            "the legacy chunk should stay pages-free (backward compat), got: $docs")
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
                // Sweep every sibling run of this test family (persistent
                // singleton container — stale rows break the global topK).
                conn.createStatement(
                    "DELETE FROM codex_documents WHERE source_document LIKE \$1 OR source_document LIKE \$2"
                )
                    .bind(0, "page-exposition-test-%")
                    .bind(1, "page-provenance-test-%")
                    .execute()
                    .awaitFirst()
            } finally {
                conn.close().awaitFirstOrNull()
            }
        }
    }
}