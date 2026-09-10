package codebase.scenarios

import codebase.store.DocumentChunk
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult

/**
 * Shared world for `@corpus-ingest` scenarios (EPIC corpus-ingest US-3, CB-FPA-RAG historic id).
 *
 * Holds the mutable state flowing between Given/When/Then steps: the
 * in-memory corpus file, the loaded chunks, the marked chunks, the
 * fake store recording the ingestion, and the last Docs section.
 * PicoContainer-scoped, one fresh instance per scenario via the
 * `@Given` init step (pattern `RagSocleWorld`).
 */
class CorpusIngestWorld {

    var loadedChunks: List<DocumentChunk> = emptyList()
    var markedChunks: List<DoubtfulChunk> = emptyList()
    var fakeStore: CorpusFakeRagStore? = null
    var docsSection: String? = null
    var docsSectionSource: List<RetrieveResult>? = null

    fun ensureInitialized() {
        // PicoContainer instantiates the world; nothing else to bootstrap.
    }
}

/**
 * In-memory corpus file content builder — mimics the codex chunks.json
 * format (US-0 compatibility confirmed) without touching the real
 * office/ corpus (zéro donnée métier dans le repo).
 */
object CorpusFileBuilder {

    fun chunksJson(count: Int, doubtful: Int): String {
        val entries = (1..count).map { i ->
            val content = if (i <= doubtful) "page [ILLISIBLE] transcription $i" else "clean content $i"
            """
            {
              "id": "chk-${i.toString().padStart(16, '0')}",
              "sourceDocument": "book",
              "sectionPath": "Chapter $i",
              "headingLevel": 1,
              "content": "$content",
              "license": "PROPRIETARY"
            }
            """.trimIndent()
        }
        return "[" + entries.joinToString(",") + "]"
    }

    fun twoChunksJson(clean: String, doubtful: String): String {
        return """
        [
          {
            "id": "chk-0000000000000001",
            "sourceDocument": "book",
            "sectionPath": "Chapter 1",
            "headingLevel": 1,
            "content": "$clean",
            "license": "PROPRIETARY"
          },
          {
            "id": "chk-0000000000000002",
            "sourceDocument": "book",
            "sectionPath": "Chapter 2",
            "headingLevel": 1,
            "content": "$doubtful",
            "license": "PROPRIETARY"
          }
        ]
        """.trimIndent()
    }
}

/**
 * Test-only [RagVectorStore] recording the doubt-aware ingestion
 * without touching pgvector (pattern `StubRagStore`). Grouping /
 * document rows are recorded in-memory for the Then steps.
 */
class CorpusFakeRagStore : RagVectorStore() {

    val ingestedByDocument: MutableMap<String, MutableList<DoubtfulChunk>> = linkedMapOf()

    fun record(marked: List<DoubtfulChunk>) {
        marked.groupBy { it.chunk.sourceDocument }.forEach { (source, chunks) ->
            ingestedByDocument.getOrPut(source) { mutableListOf() }.addAll(chunks)
        }
    }

    val documentCount: Int get() = ingestedByDocument.size
    val chunkCount: Int get() = ingestedByDocument.values.sumOf { it.size }
    val doubtfulCount: Int get() = ingestedByDocument.values.sumOf { c -> c.count { it.doubt.doubtful } }
}

/**
 * Test-only [RagVectorStore] returning canned [RetrieveResult] lists
 * for the Docs exposure scenario (pattern `StubRagStore`).
 */
class CorpusStubRagStore(
    private val results: List<RetrieveResult> = emptyList(),
) : RagVectorStore() {

    override fun searchBlocking(query: String, topK: Int): List<RetrieveResult> = results

    override fun searchWithDoubtBlocking(query: String, topK: Int): List<RetrieveResult> = results
}