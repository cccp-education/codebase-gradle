package codebase.scenarios

import codebase.store.DoubtMetadata
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult

/**
 * Shared world for `@doubt-quality` scenarios (EPIC OCR-QUALITY US-4).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 *  - the [doubt] metadata under construction;
 *  - the [doubtfulChunk] pairing chunk + doubt;
 *  - the [doubtSchema] DDL statements produced by `StoreStatements`;
 *  - the [store] stub + [doubtResults] for the search contract;
 *  - the [docsSection] produced by the `CompositeContextBuilder`
 *    Docs channel with the [excludeDoubtful] policy.
 *
 * Pattern `RagSocleWorld` (PicoContainer-scoped, one fresh instance per
 * scenario via the `@Given` init step).
 */
class DoubtQualityWorld {

    var doubt: DoubtMetadata? = null
    var doubtfulChunk: DoubtfulChunk? = null
    var doubtSchema: List<String>? = null
    var store: RagVectorStore? = null
    var doubtResults: List<RetrieveResult>? = null
    var docsSection: String? = null
    var excludeDoubtful: Boolean = false
}

/**
 * Test-only [RagVectorStore] stub — overrides the doubt-aware search to
 * return canned [RetrieveResult] lists without touching pgvector
 * (pattern `StubRagStore`). No network, no I/O.
 */
class DoubtStubRagStore(
    private val results: List<RetrieveResult> = emptyList(),
) : RagVectorStore() {

    override fun searchBlocking(query: String, topK: Int): List<RetrieveResult> = results

    override fun searchWithDoubtBlocking(query: String, topK: Int): List<RetrieveResult> = results
}