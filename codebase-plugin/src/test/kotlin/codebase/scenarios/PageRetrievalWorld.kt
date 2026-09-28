package codebase.scenarios

import codebase.rag.CompositeContextBuilder
import codebase.store.DocumentChunk
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult
import java.io.File

/**
 * Shared world for `@page-retrieval` scenarios (EPIC CB-PAGE-PROVENANCE
 * US-4, N1 scope).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 *  - the [fakeStore] recording the ingested page-carrying chunks and
 *    serving the retrieval;
 *  - the [lastResults] returned by the retrieval under test;
 *  - the [docsSection] produced by the real `CompositeContextBuilder`
 *    Docs channel (US-3 exposition).
 *
 * PicoContainer-scoped, one fresh instance per scenario (pattern
 * `CorpusIngestWorld`).
 */
class PageRetrievalWorld {

    var fakeStore: PageRetrievalFakeRagStore = PageRetrievalFakeRagStore()
    var lastResults: List<RetrieveResult>? = null
    var docsSection: String? = null

    /**
     * Ingests a chunk carrying [pages] (null → no page provenance, D5
     * backward compat). The page rides inside the [DocumentChunk] — the
     * store type owns its provenance (US-1), it is never side-channeled.
     */
    fun storeChunk(
        content: String,
        sectionPath: String,
        headingLevel: Int,
        sourceDocument: String,
        pages: List<Int>?,
    ) {
        val chunk = DocumentChunk(
            id = "chk-page-${sourceDocument}-${content.hashCode().toUInt()}",
            sourceDocument = sourceDocument,
            sectionPath = sectionPath,
            headingLevel = headingLevel,
            content = content,
            license = "Apache-2.0",
            pages = pages ?: emptyList(),
        )
        fakeStore.record(DoubtfulChunk(chunk = chunk))
    }

    fun search(query: String): List<RetrieveResult> {
        lastResults = fakeStore.searchWithDoubtBlocking(query)
        return lastResults ?: emptyList()
    }

    fun loadPageDocsContext(query: String) {
        val builder = CompositeContextBuilder(
            workspaceRoot = File(System.getProperty("java.io.tmpdir")),
            vectorStore = codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy"),
            embeddingPipeline = codebase.rag.EmbeddingPipeline(
                codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy")
            ),
            config = contracts.context.CompositeContextConfig(),
            codexStore = fakeStore,
        )
        docsSection = builder.build(query).docsSection
    }
}

/**
 * Test-only [RagVectorStore] — records the ingested page-carrying chunks
 * in memory and serves the retrieval from them (pattern
 * `CorpusFakeRagStore`). No network, no real pgvector, no Gradle task.
 *
 * The fake mirrors the real N1 contract: the retrieved
 * [RetrieveResult.pages] is read from the ingested
 * [DocumentChunk.pages] — never a parallel side channel.
 */
class PageRetrievalFakeRagStore : RagVectorStore() {

    private val recorded: MutableList<DoubtfulChunk> = mutableListOf()

    fun record(chunk: DoubtfulChunk) {
        recorded.add(chunk)
    }

    val recordedChunks: List<DoubtfulChunk> get() = recorded.toList()

    override fun searchBlocking(query: String, topK: Int): List<RetrieveResult> =
        recorded
            .filter { it.chunk.content.contains(query, ignoreCase = true) }
            .mapIndexed { index, doubtful ->
                val chunk = doubtful.chunk
                RetrieveResult(
                    chunkId = (index + 1).toLong(),
                    chunkIndex = index,
                    chunkText = chunk.content,
                    sectionPath = chunk.sectionPath,
                    headingLevel = chunk.headingLevel,
                    sourceDocument = chunk.sourceDocument,
                    similarity = 1.0,
                    confidence = doubtful.doubt.confidence,
                    doubtful = doubtful.doubt.doubtful,
                    pages = chunk.pages,
                )
            }
            .take(topK)

    override fun searchWithDoubtBlocking(query: String, topK: Int): List<RetrieveResult> =
        searchBlocking(query, topK)
}
