package codebase.store

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux

/**
 * Public contract for semantic search over the codex document corpus.
 *
 * Verbatim migration of `codex.store.CodexVectorStore` into `codebase.store`
 * as `RagVectorStore` (EPIC CDX-RAG-1, Brooklyn → Queens, N2 → N1). The
 * store belongs to the N1 socle — codebase is the canonical home of the
 * RAG infrastructure, codex delegates to it (RAG-3).
 *
 * Decoupled from Gradle tasks — usable by the N3 engine and any JVM
 * consumer. Manages its own ONNX embedding model and R2DBC pgvector
 * connection. Tables `codex_documents` / `codex_chunks` stay unchanged
 * (backward compat total, zéro re-vectorisation — Loi de l'Économie d'Encre).
 *
 * @property host PostgreSQL host (default: "localhost")
 * @property port PostgreSQL port (default: 5432)
 * @property database database name (default: "codex")
 * @property username PostgreSQL username (default: "codex")
 * @property password PostgreSQL password (default: "codex")
 */
open class RagVectorStore(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "codex",
    private val username: String = "codex",
    private val password: String = "codex"
) {
    private val model: AllMiniLmL6V2EmbeddingModel by lazy { AllMiniLmL6V2EmbeddingModel() }

    /**
     * Synchronous wrapper for [search]. Runs the coroutine-based search
     * in a [runBlocking] scope.
     *
     * @param query the search query text
     * @param topK number of results to return (default: 10)
     * @return list of [RetrieveResult] ordered by similarity descending
     */
    open fun searchBlocking(query: String, topK: Int = 10): List<RetrieveResult> =
        runBlocking { search(query, topK) }

    /**
     * Performs a semantic search against pgvector using cosine similarity.
     *
     * @param query the search query text
     * @param topK number of results to return (default: 10)
     * @return list of [RetrieveResult] ordered by similarity descending
     */
    open suspend fun search(query: String, topK: Int = 10): List<RetrieveResult> {
        val embedding = computeEmbedding(query)
        val factory = buildConnectionFactory()
        return searchSimilar(factory, embedding, topK)
    }

    /**
     * Ingests chunks with doubt metadata (EPIC OCR-QUALITY US-4).
     *
     * Groups chunks by [DocumentChunk.sourceDocument], inserts the
     * document row (with `avg_confidence` derived from its chunks), then
     * each chunk row carrying its [DoubtMetadata] — using the additive
     * [StoreStatements.doubtSchema] DDL and [StoreStatements
     * .insertChunkWithDoubt] template. The embedding pipeline and the
     * `RETURNING id` flow stay identical to the existing ingestion.
     *
     * @param chunks the chunks to ingest (with per-chunk doubt metadata)
     * @param batchLogger optional progress logger (defaults to no-op)
     * @return the number of documents ingested
     */
    open suspend fun ingestWithDoubt(
        chunks: List<DoubtfulChunk>,
        batchLogger: (String) -> Unit = {}
    ): Int {
        val factory = buildConnectionFactory()
        val conn = factory.create().awaitFirst()
        try {
            (StoreStatements.initSchema() + StoreStatements.doubtSchema())
                .forEach { conn.createStatement(it).execute().awaitFirst() }

            var docCount = 0
            for ((source, docChunks) in chunks.groupBy { it.chunk.sourceDocument }) {
                val avgConfidence = docChunks
                    .map { it.doubt.confidence }
                    .average()
                    .takeIf { !it.isNaN() } ?: DoubtMetadata.MAX_CONFIDENCE
                val docId = conn.createStatement(StoreStatements.insertDocument())
                    .bind(0, source)
                    .bind(1, docChunks.size)
                    .bind(2, docChunks.first().chunk.license)
                    .execute().awaitFirst()
                    .map { r, _ -> r.get("id", Long::class.java)!! }
                    .awaitFirst()

                for ((localIndex, entry) in docChunks.withIndex()) {
                    val chunkId = conn.createStatement(StoreStatements.insertChunkWithDoubt())
                        .bind(0, docId)
                        .bind(1, localIndex)
                        .bind(2, entry.chunk.content)
                        .bind(3, entry.chunk.sectionPath)
                        .bind(4, entry.chunk.headingLevel)
                        .bind(5, entry.doubt.confidence)
                        .bind(6, entry.doubt.doubtful)
                        .execute().awaitFirst()
                        .map { r, _ -> r.get("id", Long::class.java)!! }
                        .awaitFirst()

                    val vec = computeEmbedding(entry.chunk.content)
                    conn.createStatement(StoreStatements.updateEmbedding(vec, chunkId))
                        .execute().awaitFirst()
                }
                docCount++
                batchLogger("$source (${docChunks.size} chunks, avg_confidence=${"%.2f".format(avgConfidence)})")
            }
            return docCount
        } finally {
            conn.close().awaitFirstOrNull()
        }
    }

    /**
     * Synchronous wrapper for [searchWithDoubt] — semantic search that
     * exposes the doubt metadata of each chunk (EPIC OCR-QUALITY US-4).
     *
     * @param query the search query text
     * @param topK number of results to return (default: 10)
     * @return list of [RetrieveResult] ordered by similarity descending,
     *         each carrying its `confidence` / `doubtful` metadata
     */
    open fun searchWithDoubtBlocking(query: String, topK: Int = 10): List<RetrieveResult> =
        runBlocking { searchWithDoubt(query, topK) }

    /**
     * Semantic search exposing doubt metadata — same cosine similarity
     * ranking as [search], but the SELECT also reads the additive doubt
     * columns so the augmented context can weight or exclude shaky
     * passages.
     */
    open suspend fun searchWithDoubt(query: String, topK: Int = 10): List<RetrieveResult> {
        val embedding = computeEmbedding(query)
        val factory = buildConnectionFactory()
        return searchSimilarWithDoubt(factory, embedding, topK)
    }

    private fun buildConnectionFactory(): ConnectionFactory {
        val config = PostgresqlConnectionConfiguration.builder()
            .host(host)
            .port(port)
            .database(database)
            .username(username)
            .password(password)
            .build()
        return PostgresqlConnectionFactory(config)
    }

    private fun computeEmbedding(text: String): String {
        val embedding = model.embed(TextSegment.from(text)).content()
        return embedding.vector().joinToString(",", "[", "]")
    }

    private suspend fun searchSimilar(
        factory: ConnectionFactory,
        vectorStr: String,
        k: Int
    ): List<RetrieveResult> {
        val conn = factory.create().awaitFirst()
        try {
            val sql = """
                SELECT
                    sub.chunk_id,
                    sub.chunk_index,
                    sub.chunk_text,
                    sub.section_path,
                    sub.heading_level,
                    sub.source_document,
                    1.0 - sub.distance AS similarity
                FROM (
                    SELECT
                        c.id AS chunk_id,
                        c.chunk_index,
                        c.chunk_text,
                        c.section_path,
                        c.heading_level,
                        d.source_document,
                        c.embedding <=> ${'$'}1::vector AS distance
                    FROM codex_chunks c
                    JOIN codex_documents d ON c.document_id = d.id
                    WHERE c.embedding IS NOT NULL
                ) sub
                ORDER BY sub.distance ASC
                LIMIT ${'$'}2
            """.trimIndent()

            val result = conn.createStatement(sql)
                .bind(0, vectorStr)
                .bind(1, k)
                .execute()
                .awaitFirst()

            return Flux.from(result.map { row, _ ->
                @Suppress("UNCHECKED_CAST")
                RetrieveResult(
                    chunkId = row.get("chunk_id", Long::class.java)!!,
                    chunkIndex = (row.get("chunk_index") as Number).toInt(),
                    chunkText = row.get("chunk_text", String::class.java)!!,
                    sectionPath = row.get("section_path", String::class.java)!!,
                    headingLevel = (row.get("heading_level") as Number).toInt(),
                    sourceDocument = row.get("source_document", String::class.java)!!,
                    similarity = row.get("similarity", Double::class.java)!!
                )
            }).collectList().awaitFirst()
        } finally {
            conn.close().awaitFirstOrNull()
        }
    }

    /**
     * Semantic search with doubt columns — mirrors [searchSimilar] but
     * also reads the additive `confidence` / `doubtful` columns added by
     * [StoreStatements.doubtSchema] (EPIC OCR-QUALITY US-4).
     */
    private suspend fun searchSimilarWithDoubt(
        factory: ConnectionFactory,
        vectorStr: String,
        k: Int
    ): List<RetrieveResult> {
        val conn = factory.create().awaitFirst()
        try {
            val sql = """
                SELECT
                    sub.chunk_id,
                    sub.chunk_index,
                    sub.chunk_text,
                    sub.section_path,
                    sub.heading_level,
                    sub.source_document,
                    sub.confidence,
                    sub.doubtful,
                    1.0 - sub.distance AS similarity
                FROM (
                    SELECT
                        c.id AS chunk_id,
                        c.chunk_index,
                        c.chunk_text,
                        c.section_path,
                        c.heading_level,
                        d.source_document,
                        c.confidence,
                        c.doubtful,
                        c.embedding <=> ${'$'}1::vector AS distance
                    FROM codex_chunks c
                    JOIN codex_documents d ON c.document_id = d.id
                    WHERE c.embedding IS NOT NULL
                ) sub
                ORDER BY sub.distance ASC
                LIMIT ${'$'}2
            """.trimIndent()

            val result = conn.createStatement(sql)
                .bind(0, vectorStr)
                .bind(1, k)
                .execute()
                .awaitFirst()

            return Flux.from(result.map { row, _ ->
                @Suppress("UNCHECKED_CAST")
                RetrieveResult(
                    chunkId = row.get("chunk_id", Long::class.java)!!,
                    chunkIndex = (row.get("chunk_index") as Number).toInt(),
                    chunkText = row.get("chunk_text", String::class.java)!!,
                    sectionPath = row.get("section_path", String::class.java)!!,
                    headingLevel = (row.get("heading_level") as Number).toInt(),
                    sourceDocument = row.get("source_document", String::class.java)!!,
                    similarity = row.get("similarity", Double::class.java)!!,
                    confidence = row.get("confidence", Double::class.java)
                        ?: DoubtMetadata.MAX_CONFIDENCE,
                    doubtful = row.get("doubtful", Boolean::class.java) ?: false
                )
            }).collectList().awaitFirst()
        } finally {
            conn.close().awaitFirstOrNull()
        }
    }
}