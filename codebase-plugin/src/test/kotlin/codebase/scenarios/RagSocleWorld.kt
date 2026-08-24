package codebase.scenarios

import codebase.store.RagVectorStore
import codebase.store.RetrieveResult
import java.io.File

/**
 * Shared world for `@rag-socle` scenarios (EPIC CDX-RAG-SOCLE US-5).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 *  - the [store] under test (real [RagVectorStore] with defaults/custom
 *    params, or a [StubRagStore] returning canned results / throwing);
 *  - the last [retrieveResult] built from Given steps;
 *  - the last [docsSection] produced by `CompositeContextBuilder.loadDocsContext`.
 *
 * Pattern `FineTuningWorld` (PicoContainer-scoped, one fresh instance per
 * scenario via the `@Given` init step).
 */
class RagSocleWorld {

    var store: RagVectorStore? = null
    var retrieveResult: RetrieveResult? = null
    var docsSection: String? = null

    fun ensureInitialized() {
        // PicoContainer instantiates the world; nothing else to bootstrap.
    }
}

/**
 * Test-only [RagVectorStore] stub — overrides [searchBlocking] to return
 * canned [RetrieveResult] lists or throw, without touching pgvector.
 * No network, no I/O (pattern `StubRegistryClient`).
 */
class StubRagStore(
    private val results: List<RetrieveResult> = emptyList(),
    private val failure: Throwable? = null,
) : RagVectorStore() {

    override fun searchBlocking(query: String, topK: Int): List<RetrieveResult> {
        failure?.let { throw it }
        return results
    }
}