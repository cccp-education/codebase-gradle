package codebase.koog.agentic

import codebase.koog.session.MigrationRunner
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono
import java.time.Instant

interface AgenticChunkRepository {

    suspend fun initSchema(): Unit = error("Not supported")

    suspend fun insertChunk(chunk: OntologizedChunk): Boolean = error("Not supported")

    suspend fun insertChunks(chunks: List<OntologizedChunk>): Int = error("Not supported")

    suspend fun getChunk(id: String): OntologizedChunk? = error("Not supported")

    suspend fun listChunks(limit: Int = 100): List<OntologizedChunk> = error("Not supported")

    suspend fun listChunksByDomain(domain: String, limit: Int = 100): List<OntologizedChunk> = error("Not supported")

    suspend fun listChunksByVerb(verb: TaxonomyVerb, limit: Int = 100): List<OntologizedChunk> = error("Not supported")

    suspend fun listChunksByDagLevel(level: DagLevel, limit: Int = 100): List<OntologizedChunk> = error("Not supported")

    suspend fun listChunksByTaxonomySection(section: TaxonomySection, limit: Int = 100): List<OntologizedChunk> = error("Not supported")

    suspend fun insertRelation(
        sourceChunkId: String,
        targetChunkId: String,
        relationType: ChunkRelationType,
        confidence: Double = 0.5
    ): Long = error("Not supported")

    suspend fun insertRelations(relations: List<ChunkRelation>): Int = error("Not supported")

    suspend fun getRelations(sourceChunkId: String): List<ChunkRelation> = error("Not supported")

    suspend fun updateEmbedding(id: String, vectorStr: String): Boolean = error("Not supported")

    suspend fun countChunks(): Int = error("Not supported")

    suspend fun countRelations(): Int = error("Not supported")

    companion object {
        operator fun invoke(connectionFactory: ConnectionFactory): AgenticChunkRepository =
            R2dbcAgenticChunkRepository(connectionFactory)
    }
}

enum class ChunkRelationType {
    ENFORCES,
    CONFLICTS_WITH,
    DEPENDS_ON,
    REFINES
}

data class ChunkRelation(
    val id: Long,
    val sourceChunkId: String,
    val targetChunkId: String,
    val relationType: ChunkRelationType,
    val confidence: Double,
    val createdAt: Instant
)

private class R2dbcAgenticChunkRepository(
    private val connectionFactory: ConnectionFactory
) : AgenticChunkRepository {

    private suspend fun <R> withConnection(block: suspend (conn: Connection) -> R): R {
        val conn = Mono.from(connectionFactory.create()).awaitSingle()
        try {
            return block(conn)
        } finally {
            Mono.from(conn.close()).subscribe()
        }
    }

    override suspend fun initSchema() {
        MigrationRunner(connectionFactory).migrate()
    }

    override suspend fun insertChunk(chunk: OntologizedChunk): Boolean {
        return withConnection { conn ->
            val s = conn.createStatement(
                "INSERT INTO agentic_chunks" +
                " (id, source_file, source_lines, chunk_type, content, verb, domain," +
                "  dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence)" +
                " VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)" +
                " ON CONFLICT (id) DO NOTHING"
            )
            val c = chunk.chunk
            s.bind("$1", c.id)
            s.bind("$2", c.sourceFile)
            s.bind("$3", c.sourceLines)
            s.bind("$4", c.chunkType.name)
            s.bind("$5", c.content)
            if (c.verb != null) s.bind("$6", c.verb.name) else s.bindNull("$6", String::class.java)
            if (c.domain != null) s.bind("$7", c.domain) else s.bindNull("$7", String::class.java)
            if (c.dagLevel != null) s.bind("$8", c.dagLevel.name) else s.bindNull("$8", String::class.java)
            if (c.circle != null) s.bind("$9", c.circle) else s.bindNull("$9", Int::class.javaObjectType)
            s.bind("$10", c.weight)
            s.bind("$11", c.checksum)
            s.bind("$12", chunk.taxonomySection.name)
            s.bind("$13", chunk.ontologyConfidence)

            val updated = Mono.from(s.execute())
                .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L)
                .awaitSingle()
            updated > 0L
        }
    }

    override suspend fun insertChunks(chunks: List<OntologizedChunk>): Int {
        var inserted = 0
        for (chunk in chunks) {
            if (insertChunk(chunk)) inserted++
        }
        return inserted
    }

    override suspend fun getChunk(id: String): OntologizedChunk? {
        return withConnection { conn ->
            val list: List<OntologizedChunk> = Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks WHERE id = $1"
                )
                    .bind("$1", id)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
            list.firstOrNull()
        }
    }

    override suspend fun listChunks(limit: Int): List<OntologizedChunk> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks ORDER BY created_at DESC LIMIT $1"
                )
                    .bind("$1", limit)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun listChunksByDomain(domain: String, limit: Int): List<OntologizedChunk> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks WHERE domain = $1 ORDER BY created_at DESC LIMIT $2"
                )
                    .bind("$1", domain)
                    .bind("$2", limit)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun listChunksByVerb(verb: TaxonomyVerb, limit: Int): List<OntologizedChunk> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks WHERE verb = $1 ORDER BY created_at DESC LIMIT $2"
                )
                    .bind("$1", verb.name)
                    .bind("$2", limit)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun listChunksByDagLevel(level: DagLevel, limit: Int): List<OntologizedChunk> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks WHERE dag_level = $1 ORDER BY created_at DESC LIMIT $2"
                )
                    .bind("$1", level.name)
                    .bind("$2", limit)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun listChunksByTaxonomySection(section: TaxonomySection, limit: Int): List<OntologizedChunk> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_file, source_lines, chunk_type, content, verb, domain," +
                    " dag_level, circle, weight, checksum, taxonomy_section, ontology_confidence," +
                    " valid_from, valid_until, created_at" +
                    " FROM agentic_chunks WHERE taxonomy_section = $1 ORDER BY created_at DESC LIMIT $2"
                )
                    .bind("$1", section.name)
                    .bind("$2", limit)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToOntologizedChunk(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun insertRelation(
        sourceChunkId: String,
        targetChunkId: String,
        relationType: ChunkRelationType,
        confidence: Double
    ): Long {
        return withConnection { conn ->
            val s = conn.createStatement(
                "INSERT INTO chunk_relations" +
                " (source_chunk_id, target_chunk_id, relation_type, confidence)" +
                " VALUES ($1, $2, $3, $4)" +
                " ON CONFLICT (source_chunk_id, target_chunk_id, relation_type) DO UPDATE" +
                " SET confidence = EXCLUDED.confidence" +
                " RETURNING id"
            )
            s.bind("$1", sourceChunkId)
            s.bind("$2", targetChunkId)
            s.bind("$3", relationType.name)
            s.bind("$4", confidence)

            Mono.from(s.execute())
                .flatMap { result ->
                    Mono.from(result.map { row, _ -> row.get("id", Long::class.java)!! })
                }
                .awaitSingle()
        }
    }

    override suspend fun insertRelations(relations: List<ChunkRelation>): Int {
        var inserted = 0
        for (rel in relations) {
            try {
                insertRelation(rel.sourceChunkId, rel.targetChunkId, rel.relationType, rel.confidence)
                inserted++
            } catch (_: Exception) {
            }
        }
        return inserted
    }

    override suspend fun getRelations(sourceChunkId: String): List<ChunkRelation> {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement(
                    "SELECT id, source_chunk_id, target_chunk_id, relation_type, confidence, created_at" +
                    " FROM chunk_relations WHERE source_chunk_id = $1 ORDER BY created_at"
                )
                    .bind("$1", sourceChunkId)
                    .execute()
            ).flatMapMany { result ->
                result.map { row, _ -> mapToChunkRelation(row) }
            }.collectList().awaitSingle()
        }
    }

    override suspend fun updateEmbedding(id: String, vectorStr: String): Boolean {
        return withConnection { conn ->
            val s = conn.createStatement(
                "UPDATE agentic_chunks SET embedding = CAST($2 AS vector) WHERE id = $1"
            )
            s.bind("$1", id)
            s.bind("$2", vectorStr)
            val updated = Mono.from(s.execute())
                .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L)
                .awaitSingle()
            updated > 0L
        }
    }

    override suspend fun countChunks(): Int {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement("SELECT count(*) FROM agentic_chunks").execute()
            ).flatMap { result ->
                Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
            }.awaitSingle().toInt()
        }
    }

    override suspend fun countRelations(): Int {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement("SELECT count(*) FROM chunk_relations").execute()
            ).flatMap { result ->
                Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
            }.awaitSingle().toInt()
        }
    }

    private fun mapToOntologizedChunk(row: io.r2dbc.spi.Row): OntologizedChunk {
        val chunk = AgenticChunk(
            id = row.get("id", String::class.java)!!,
            sourceFile = row.get("source_file", String::class.java)!!,
            sourceLines = row.get("source_lines", String::class.java)!!,
            chunkType = ChunkType.valueOf(row.get("chunk_type", String::class.java)!!),
            content = row.get("content", String::class.java)!!,
            verb = row.get("verb", String::class.java)?.let { TaxonomyVerb.valueOf(it) },
            domain = row.get("domain", String::class.java),
            dagLevel = row.get("dag_level", String::class.java)?.let { DagLevel.valueOf(it) },
            circle = row.get("circle", Int::class.javaObjectType),
            weight = row.get("weight", Double::class.java) ?: 0.5,
            checksum = row.get("checksum", String::class.java)!!
        )
        return OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.valueOf(row.get("taxonomy_section", String::class.java) ?: "UNKNOWN"),
            ontologyConfidence = row.get("ontology_confidence", Double::class.java) ?: 0.0,
            relatedChunkIds = emptyList()
        )
    }

    private fun mapToChunkRelation(row: io.r2dbc.spi.Row): ChunkRelation {
        return ChunkRelation(
            id = row.get("id", Long::class.java)!!,
            sourceChunkId = row.get("source_chunk_id", String::class.java)!!,
            targetChunkId = row.get("target_chunk_id", String::class.java)!!,
            relationType = ChunkRelationType.valueOf(row.get("relation_type", String::class.java)!!),
            confidence = row.get("confidence", Double::class.java) ?: 0.5,
            createdAt = row.get("created_at", Instant::class.java)!!
        )
    }
}
