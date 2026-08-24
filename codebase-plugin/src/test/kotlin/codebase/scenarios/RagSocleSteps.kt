package codebase.scenarios

import codebase.rag.CompositeContextBuilder
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@rag-socle` scenarios (EPIC CDX-RAG-SOCLE US-5).
 *
 * Steps are prefixed "rag socle" / "retrieve result" / "docs section" /
 * "composite context builder" to avoid glue collisions with other feature
 * step classes sharing the `codebase.scenarios` package (pattern S-088 —
 * FineTuningSteps, SubgraphSteps).
 *
 * Pure BDD: no Gradle task is invoked, no network call is issued — the
 * scenarios drive the domain `codebase.store` via the real
 * [RagVectorStore] (instantiated, never queried) or a [StubRagStore]
 * returning canned [RetrieveResult] lists.
 */
class RagSocleSteps(private val world: RagSocleWorld) {

    @Given("a rag socle world is initialized")
    fun `rag socle world initialized`() {
        world.ensureInitialized()
        assertNotNull(world, "RagSocleWorld should be instantiated by PicoContainer")
    }

    @When("the rag socle store is instantiated with defaults")
    fun `rag socle store instantiated with defaults`() {
        world.store = RagVectorStore()
    }

    @When("the rag socle store is instantiated with host {string}, port {int}, database {string}, username {string}, password {string}")
    fun `rag socle store instantiated with custom params`(
        host: String, port: Int, database: String, username: String, password: String
    ) {
        world.store = RagVectorStore(
            host = host, port = port, database = database,
            username = username, password = password
        )
    }

    @Then("the rag socle store is non-null")
    fun `rag socle store non-null`() {
        assertNotNull(world.store, "RagVectorStore should be instantiated")
    }

    @And("the rag socle store class is codebase.store.RagVectorStore")
    fun `rag socle store class`() {
        assertEquals("codebase.store.RagVectorStore", world.store!!.javaClass.name)
    }

    @Given("a retrieve result with chunkId {long}, chunkIndex {int}, chunkText {string}, sectionPath {string}, headingLevel {int}, sourceDocument {string}, similarity {double}")
    fun `retrieve result given`(
        chunkId: Long, chunkIndex: Int, chunkText: String, sectionPath: String,
        headingLevel: Int, sourceDocument: String, similarity: Double
    ) {
        world.retrieveResult = RetrieveResult(
            chunkId = chunkId, chunkIndex = chunkIndex, chunkText = chunkText,
            sectionPath = sectionPath, headingLevel = headingLevel,
            sourceDocument = sourceDocument, similarity = similarity
        )
    }

    @Then("the retrieve result chunkId is {long}")
    fun `retrieve result chunkId`(expected: Long) {
        assertEquals(expected, world.retrieveResult!!.chunkId)
    }

    @And("the retrieve result chunkText is {string}")
    fun `retrieve result chunkText`(expected: String) {
        assertEquals(expected, world.retrieveResult!!.chunkText)
    }

    @And("the retrieve result similarity is {double}")
    fun `retrieve result similarity`(expected: Double) {
        assertEquals(expected, world.retrieveResult!!.similarity)
    }

    @And("the retrieve result class is codebase.store.RetrieveResult")
    fun `retrieve result class`() {
        assertEquals("codebase.store.RetrieveResult", world.retrieveResult!!.javaClass.name)
    }

    @When("the composite context builder is built with a null rag store")
    fun `composite context builder null store`() {
        // CompositeContextBuilder codexStore param is nullable — the null
        // path produces the "not configured" Docs fallback message.
        // We don't need a real VectorStore/EmbeddingPipeline for Docs-only.
    }

    @And("the docs context is loaded for query {string}")
    fun `docs context loaded`(query: String) {
        val builder = CompositeContextBuilder(
            workspaceRoot = File(System.getProperty("java.io.tmpdir")),
            vectorStore = codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy"),
            embeddingPipeline = codebase.rag.EmbeddingPipeline(
                codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy")
            ),
            config = contracts.context.CompositeContextConfig(),
            codexStore = world.store,
        )
        // build() exercises assembleContext which degrades RAG gracefully
        // (dummy VectorStore throws, caught) and populates docsSection via
        // loadDocsContext (the socle store under test).
        val composite = builder.build(query)
        world.docsSection = composite.docsSection
    }

    @Then("the docs section reports the rag store is not configured")
    fun `docs section not configured`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains("non configure", ignoreCase = true),
            "Expected 'non configure' in docs section, got: $docs")
    }

    @Given("a rag store stub that throws on search")
    fun `rag store stub throws`() {
        world.store = StubRagStore(failure = RuntimeException("pgvector connection refused"))
    }

    @When("the composite context builder is built with the throwing rag store stub")
    fun `composite context builder throwing store`() {
        // store already set in Given; the build step loads docs context.
    }

    @Then("the docs section reports the rag store is unavailable")
    fun `docs section unavailable`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains("indisponible", ignoreCase = true),
            "Expected 'indisponible' in docs section, got: $docs")
    }

    @Given("a rag store stub returning {int} results for query {string}")
    fun `rag store stub returning`(count: Int, query: String) {
        val results = (1..count).map { i ->
            RetrieveResult(
                chunkId = i.toLong(), chunkIndex = i - 1, chunkText = "result $i for $query",
                sectionPath = "Section $i", headingLevel = 1,
                sourceDocument = "doc.pdf", similarity = 1.0 - (i * 0.1)
            )
        }
        world.store = StubRagStore(results = results)
    }

    @When("the composite context builder is built with the returning rag store stub")
    fun `composite context builder returning store`() {
        // store already set in Given; the build step loads docs context.
    }

    @Then("the docs section contains {int} result entries")
    fun `docs section count`(expected: Int) {
        val docs = world.docsSection!!
        val markers = docs.split("[Doc] source=").toSet()
        // The first segment is the intro line; count the rest.
        assertEquals(expected, markers.size - 1,
            "Expected $expected result entries, got: ${markers.size - 1} in: $docs")
    }

    @And("the docs section contains the source document {string}")
    fun `docs section source document`(expected: String) {
        val docs = world.docsSection!!
        assertTrue(docs.contains(expected),
            "Expected source document '$expected' in docs section, got: $docs")
    }
}