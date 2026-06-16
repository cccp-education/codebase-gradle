package codebase.koog.agentic

import codebase.infrastructure.PostgresFixture
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

class AgenticChunkRepositoryTest {

    companion object {
        lateinit var connectionFactory: ConnectionFactory
        lateinit var repository: AgenticChunkRepository

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
            repository = AgenticChunkRepository(connectionFactory)
        }
    }

    @BeforeEach
    fun cleanDatabase() {
        runBlocking {
            val conn = Mono.from(connectionFactory.create()).awaitSingle()
            try {
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS chunk_relations").execute()).awaitSingle()
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS agentic_chunks").execute()).awaitSingle()
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS vibecoding_steps").execute()).awaitSingle()
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS vibecoding_sessions").execute()).awaitSingle()
                Mono.from(conn.createStatement("DROP TABLE IF EXISTS schema_version").execute()).awaitSingle()
            } finally {
                Mono.from(conn.close()).subscribe()
            }
        }
    }

    private fun buildTestChunk(
        id: String = "test-id-1",
        content: String = "== Principes Fondateurs\n. Le verbe dit pourquoi.",
        verb: TaxonomyVerb? = TaxonomyVerb.GENERER,
        domain: String? = "codebase",
        dagLevel: DagLevel? = DagLevel.N1,
        circle: Int? = 4,
        chunkType: ChunkType = ChunkType.CONCEPT,
        taxonomySection: TaxonomySection = TaxonomySection.PRINCIPES,
        confidence: Double = 0.8
    ): OntologizedChunk {
        val agenticChunk = AgenticChunk(
            id = id,
            sourceFile = "TAXONOMIE_WORKSPACE.adoc",
            sourceLines = "42-48",
            chunkType = chunkType,
            content = content,
            verb = verb,
            domain = domain,
            dagLevel = dagLevel,
            circle = circle,
            weight = 1.0,
            checksum = "abc123def456"
        )
        return OntologizedChunk(
            chunk = agenticChunk,
            taxonomySection = taxonomySection,
            ontologyConfidence = confidence,
            relatedChunkIds = emptyList()
        )
    }

    @Test
    fun `should init schema via MigrationRunner`() {
        runBlocking {
            repository.initSchema()

            val conn = Mono.from(connectionFactory.create()).awaitSingle()
            try {
                val tables = Mono.from(
                    conn.createStatement(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('agentic_chunks', 'chunk_relations', 'schema_version')"
                    ).execute()
                ).flatMapMany { result ->
                    result.map { row, _ -> row.get(0, String::class.java)!! }
                }.collectList().awaitSingle()

                assertTrue(tables.contains("agentic_chunks"), "agentic_chunks table should exist")
                assertTrue(tables.contains("chunk_relations"), "chunk_relations table should exist")
                assertTrue(tables.contains("schema_version"), "schema_version table should exist")
            } finally {
                Mono.from(conn.close()).subscribe()
            }
        }
    }

    @Test
    fun `should insert and retrieve a chunk`() {
        runBlocking {
            repository.initSchema()
            val chunk = buildTestChunk()

            val inserted = repository.insertChunk(chunk)
            assertTrue(inserted, "Should insert chunk successfully")

            val retrieved = repository.getChunk(chunk.chunk.id)
            assertNotNull(retrieved, "Should retrieve inserted chunk")
            assertEquals(chunk.chunk.id, retrieved!!.chunk.id)
            assertEquals(chunk.chunk.content, retrieved.chunk.content)
            assertEquals(chunk.chunk.verb, retrieved.chunk.verb)
            assertEquals(chunk.chunk.domain, retrieved.chunk.domain)
            assertEquals(chunk.chunk.dagLevel, retrieved.chunk.dagLevel)
            assertEquals(chunk.chunk.circle, retrieved.chunk.circle)
            assertEquals(chunk.taxonomySection, retrieved.taxonomySection)
            assertEquals(chunk.ontologyConfidence, retrieved.ontologyConfidence, 0.01)
        }
    }

    @Test
    fun `should insert chunks in batch`() {
        runBlocking {
            repository.initSchema()
            val chunks = listOf(
                buildTestChunk(id = "batch-1", content = "== Principes\n. Fondateur."),
                buildTestChunk(id = "batch-2", content = "== Taxonomie\n. Quatre Verbes.", taxonomySection = TaxonomySection.TAXONOMIE),
                buildTestChunk(id = "batch-3", content = "== Format Pivot\n. metadata.json.", taxonomySection = TaxonomySection.FORMAT_PIVOT)
            )

            val inserted = repository.insertChunks(chunks)
            assertEquals(3, inserted, "Should insert all 3 chunks")

            val count = repository.countChunks()
            assertEquals(3, count, "Should have 3 chunks in DB")
        }
    }

    @Test
    fun `should be idempotent on duplicate insert`() {
        runBlocking {
            repository.initSchema()
            val chunk = buildTestChunk(id = "dup-test")

            val first = repository.insertChunk(chunk)
            assertTrue(first, "First insert should succeed")

            val second = repository.insertChunk(chunk)
            assertFalse(second, "Second insert of same id should be no-op")

            val count = repository.countChunks()
            assertEquals(1, count, "Should still have only 1 chunk")
        }
    }

    @Test
    fun `should return null for non-existent chunk`() {
        runBlocking {
            repository.initSchema()
            val result = repository.getChunk("non-existent-id")
            assertNull(result, "Non-existent chunk should return null")
        }
    }

    @Test
    fun `should list chunks ordered by created_at DESC`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "list-1", content = "First chunk"))
            repository.insertChunk(buildTestChunk(id = "list-2", content = "Second chunk"))
            repository.insertChunk(buildTestChunk(id = "list-3", content = "Third chunk"))

            val chunks = repository.listChunks(limit = 10)
            assertEquals(3, chunks.size, "Should list all 3 chunks")
            assertEquals("list-3", chunks[0].chunk.id, "Most recent should be first")
        }
    }

    @Test
    fun `should list chunks by domain`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "dom-1", domain = "codebase"))
            repository.insertChunk(buildTestChunk(id = "dom-2", domain = "planner"))
            repository.insertChunk(buildTestChunk(id = "dom-3", domain = "codebase"))

            val codebaseChunks = repository.listChunksByDomain("codebase")
            assertEquals(2, codebaseChunks.size, "Should find 2 codebase chunks")

            val plannerChunks = repository.listChunksByDomain("planner")
            assertEquals(1, plannerChunks.size, "Should find 1 planner chunk")
        }
    }

    @Test
    fun `should list chunks by verb`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "verb-1", verb = TaxonomyVerb.GENERER))
            repository.insertChunk(buildTestChunk(id = "verb-2", verb = TaxonomyVerb.INTERDIRE, chunkType = ChunkType.RULE))
            repository.insertChunk(buildTestChunk(id = "verb-3", verb = TaxonomyVerb.GENERER))

            val genererChunks = repository.listChunksByVerb(TaxonomyVerb.GENERER)
            assertEquals(2, genererChunks.size, "Should find 2 GENERER chunks")

            val interdireChunks = repository.listChunksByVerb(TaxonomyVerb.INTERDIRE)
            assertEquals(1, interdireChunks.size, "Should find 1 INTERDIRE chunk")
        }
    }

    @Test
    fun `should list chunks by DAG level`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "dag-1", dagLevel = DagLevel.N1))
            repository.insertChunk(buildTestChunk(id = "dag-2", dagLevel = DagLevel.N2))
            repository.insertChunk(buildTestChunk(id = "dag-3", dagLevel = DagLevel.N1))

            val n1Chunks = repository.listChunksByDagLevel(DagLevel.N1)
            assertEquals(2, n1Chunks.size, "Should find 2 N1 chunks")

            val n2Chunks = repository.listChunksByDagLevel(DagLevel.N2)
            assertEquals(1, n2Chunks.size, "Should find 1 N2 chunk")
        }
    }

    @Test
    fun `should list chunks by taxonomy section`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "tax-1", taxonomySection = TaxonomySection.PRINCIPES))
            repository.insertChunk(buildTestChunk(id = "tax-2", taxonomySection = TaxonomySection.TAXONOMIE))
            repository.insertChunk(buildTestChunk(id = "tax-3", taxonomySection = TaxonomySection.PRINCIPES))

            val principesChunks = repository.listChunksByTaxonomySection(TaxonomySection.PRINCIPES)
            assertEquals(2, principesChunks.size, "Should find 2 PRINCIPES chunks")

            val taxonomieChunks = repository.listChunksByTaxonomySection(TaxonomySection.TAXONOMIE)
            assertEquals(1, taxonomieChunks.size, "Should find 1 TAXONOMIE chunk")
        }
    }

    @Test
    fun `should respect limit parameter`() {
        runBlocking {
            repository.initSchema()
            for (i in 1..5) {
                repository.insertChunk(buildTestChunk(id = "limit-$i", content = "Chunk $i"))
            }

            val limited = repository.listChunks(limit = 3)
            assertEquals(3, limited.size, "Should respect limit of 3")
        }
    }

    @Test
    fun `should insert and retrieve relations`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "rel-src", content = "Source chunk"))
            repository.insertChunk(buildTestChunk(id = "rel-tgt", content = "Target chunk"))

            val relId = repository.insertRelation("rel-src", "rel-tgt", ChunkRelationType.DEPENDS_ON, 0.9)
            assertTrue(relId > 0, "Should insert relation with valid id")

            val relations = repository.getRelations("rel-src")
            assertEquals(1, relations.size, "Should find 1 relation")
            assertEquals("rel-src", relations[0].sourceChunkId)
            assertEquals("rel-tgt", relations[0].targetChunkId)
            assertEquals(ChunkRelationType.DEPENDS_ON, relations[0].relationType)
            assertEquals(0.9, relations[0].confidence, 0.01)
        }
    }

    @Test
    fun `should upsert relation on conflict`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "upsert-src"))
            repository.insertChunk(buildTestChunk(id = "upsert-tgt"))

            val firstId = repository.insertRelation("upsert-src", "upsert-tgt", ChunkRelationType.ENFORCES, 0.5)
            val secondId = repository.insertRelation("upsert-src", "upsert-tgt", ChunkRelationType.ENFORCES, 0.9)

            assertEquals(firstId, secondId, "Upsert should return same id")

            val relations = repository.getRelations("upsert-src")
            assertEquals(1, relations.size, "Should still have only 1 relation")
            assertEquals(0.9, relations[0].confidence, 0.01, "Confidence should be updated")
        }
    }

    @Test
    fun `should insert multiple relation types between same chunks`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "multi-src"))
            repository.insertChunk(buildTestChunk(id = "multi-tgt"))

            repository.insertRelation("multi-src", "multi-tgt", ChunkRelationType.DEPENDS_ON, 0.8)
            repository.insertRelation("multi-src", "multi-tgt", ChunkRelationType.REFINES, 0.6)

            val relations = repository.getRelations("multi-src")
            assertEquals(2, relations.size, "Should have 2 different relation types")
            val types = relations.map { it.relationType }.toSet()
            assertTrue(types.contains(ChunkRelationType.DEPENDS_ON))
            assertTrue(types.contains(ChunkRelationType.REFINES))
        }
    }

    @Test
    fun `should insert relations in batch`() {
        runBlocking {
            repository.initSchema()
            repository.insertChunk(buildTestChunk(id = "batch-src"))
            repository.insertChunk(buildTestChunk(id = "batch-tgt1"))
            repository.insertChunk(buildTestChunk(id = "batch-tgt2"))

            val relations = listOf(
                ChunkRelation(0, "batch-src", "batch-tgt1", ChunkRelationType.ENFORCES, 0.7, java.time.Instant.now()),
                ChunkRelation(0, "batch-src", "batch-tgt2", ChunkRelationType.DEPENDS_ON, 0.5, java.time.Instant.now())
            )

            val inserted = repository.insertRelations(relations)
            assertEquals(2, inserted, "Should insert 2 relations")

            val count = repository.countRelations()
            assertEquals(2, count, "Should have 2 relations in DB")
        }
    }

    @Test
    fun `should update embedding vector`() {
        runBlocking {
            repository.initSchema()
            val chunk = buildTestChunk(id = "emb-test")
            repository.insertChunk(chunk)

            val vectorStr = "[" + (1..384).joinToString(",") { "0.1" } + "]"
            val updated = repository.updateEmbedding("emb-test", vectorStr)
            assertTrue(updated, "Should update embedding successfully")

            val conn = Mono.from(connectionFactory.create()).awaitSingle()
            try {
                val hasEmbedding = Mono.from(
                    conn.createStatement(
                        "SELECT count(*) FROM agentic_chunks WHERE id = $1 AND embedding IS NOT NULL"
                    )
                        .bind("$1", "emb-test")
                        .execute()
                ).flatMap { result ->
                    Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
                }.awaitSingle()
                assertEquals(1L, hasEmbedding, "Embedding should be non-null")
            } finally {
                Mono.from(conn.close()).subscribe()
            }
        }
    }

    @Test
    fun `should count chunks and relations`() {
        runBlocking {
            repository.initSchema()
            assertEquals(0, repository.countChunks(), "Should start with 0 chunks")
            assertEquals(0, repository.countRelations(), "Should start with 0 relations")

            repository.insertChunk(buildTestChunk(id = "count-1"))
            repository.insertChunk(buildTestChunk(id = "count-2"))
            repository.insertChunk(buildTestChunk(id = "count-3"))

            assertEquals(3, repository.countChunks(), "Should count 3 chunks")

            repository.insertRelation("count-1", "count-2", ChunkRelationType.DEPENDS_ON)
            repository.insertRelation("count-2", "count-3", ChunkRelationType.REFINES)

            assertEquals(2, repository.countRelations(), "Should count 2 relations")
        }
    }

    @Test
    fun `should store chunk with all nullable fields null`() {
        runBlocking {
            repository.initSchema()
            val chunk = buildTestChunk(
                id = "nullable-test",
                verb = null,
                domain = null,
                dagLevel = null,
                circle = null
            )

            repository.insertChunk(chunk)
            val retrieved = repository.getChunk("nullable-test")
            assertNotNull(retrieved)
            assertNull(retrieved!!.chunk.verb)
            assertNull(retrieved.chunk.domain)
            assertNull(retrieved.chunk.dagLevel)
            assertNull(retrieved.chunk.circle)
        }
    }

    @Test
    fun `should store all chunk types`() {
        runBlocking {
            repository.initSchema()
            val types = listOf(
                buildTestChunk(id = "type-rule", chunkType = ChunkType.RULE, verb = TaxonomyVerb.INTERDIRE),
                buildTestChunk(id = "type-concept", chunkType = ChunkType.CONCEPT),
                buildTestChunk(id = "type-procedure", chunkType = ChunkType.PROCEDURE, verb = TaxonomyVerb.VALIDER),
                buildTestChunk(id = "type-metadata", chunkType = ChunkType.METADATA, verb = null),
                buildTestChunk(id = "type-constraint", chunkType = ChunkType.CONSTRAINT, verb = TaxonomyVerb.VALIDER)
            )

            repository.insertChunks(types)
            assertEquals(5, repository.countChunks())

            for (type in types) {
                val retrieved = repository.getChunk(type.chunk.id)
                assertNotNull(retrieved, "Should retrieve chunk of type ${type.chunk.chunkType}")
                assertEquals(type.chunk.chunkType, retrieved!!.chunk.chunkType)
            }
        }
    }

    @Test
    fun `should store all taxonomy sections`() {
        runBlocking {
            repository.initSchema()
            val sections = TaxonomySection.entries.filter { it != TaxonomySection.UNKNOWN }

            for ((i, section) in sections.withIndex()) {
                repository.insertChunk(
                    buildTestChunk(
                        id = "section-$i",
                        content = "== ${section.name}\n. Test content.",
                        taxonomySection = section
                    )
                )
            }

            assertEquals(sections.size, repository.countChunks())

            for (section in sections) {
                val chunks = repository.listChunksByTaxonomySection(section)
                assertTrue(chunks.isNotEmpty(), "Should find chunks for section $section")
                assertEquals(section, chunks[0].taxonomySection)
            }
        }
    }
}
