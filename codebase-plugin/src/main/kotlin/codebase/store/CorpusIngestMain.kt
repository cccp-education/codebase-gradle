package codebase.store

import codebase.rag.StdoutFormatter
import java.io.File

/**
 * Entry point for the corpus FPA ingestion run (EPIC CB-FPA-RAG US-1).
 *
 * Loads the codex chunks.json corpus file, derives the doubt metadata
 * via [DoubtPolicy.mark] (OCR `[ILLISIBLE]` markers), then ingests into
 * pgvector through the existing [RagVectorStore.ingestWithDoubt] socle
 * (S-213). No code in the ingestion path — configuration + run only,
 * zéro donnée métier committée (frontière licence S-070).
 *
 * Environment variables:
 *  - `CORPUS_CHUNKS_FILE` — path to the codex chunks.json (required)
 *  - `PGVECTOR_HOST` / `PGVECTOR_PORT` / `PGVECTOR_DB` — pgvector
 *    connection (defaults: localhost / 5432 / codex)
 *  - `PGVECTOR_USER` / `PGVECTOR_PASSWORD` — pgvector credentials
 *    (defaults: codex / codex)
 */
object CorpusIngestMain {

    @JvmStatic
    fun main(args: Array<String>) {
        val chunksFile = System.getenv("CORPUS_CHUNKS_FILE")
            ?.let(::File)
            ?: error("CORPUS_CHUNKS_FILE env var required (path to codex chunks.json)")

        val store = RagVectorStore(
            host = System.getenv("PGVECTOR_HOST") ?: "localhost",
            port = System.getenv("PGVECTOR_PORT")?.toIntOrNull() ?: 5432,
            database = System.getenv("PGVECTOR_DB") ?: "codex",
            username = System.getenv("PGVECTOR_USER") ?: "codex",
            password = System.getenv("PGVECTOR_PASSWORD") ?: "codex"
        )

        run(chunksFile, store)
    }

    /** Ingests the corpus file into pgvector, doubt-aware. */
    fun run(chunksFile: File, store: RagVectorStore): Int {
        val chunks = CorpusChunksLoader.load(chunksFile)
        val doubtfulCount = chunks.count {
            it.content.contains(DoubtPolicy.ILLISIBLE_MARKER)
        }
        StdoutFormatter.banner("Corpus Ingest — CB-FPA-RAG US-1")
        StdoutFormatter.ctx("${chunks.size} chunks loaded from ${chunksFile.name}")
        StdoutFormatter.plan("$doubtfulCount doubtful chunks (OCR marker) — ingestWithDoubt")

        val docCount = kotlinx.coroutines.runBlocking {
            store.ingestWithDoubt(DoubtPolicy.mark(chunks)) { doc ->
                StdoutFormatter.result(doc)
            }
        }

        StdoutFormatter.separator()
        StdoutFormatter.result("$docCount documents ingested, ${chunks.size} chunks embedded")
        return chunks.size
    }
}