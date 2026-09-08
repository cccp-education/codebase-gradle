package codebase.scenarios

import codebase.rag.CompositeContextBuilder
import codebase.store.DoubtMetadata
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@doubt-quality` scenarios (EPIC OCR-QUALITY US-4).
 *
 * Steps are prefixed "doubt quality" / "doubt metadata" / "doubtful
 * chunk" / "doubt-aware" to avoid glue collisions with other feature
 * step classes sharing the `codebase.scenarios` package (pattern S-088).
 *
 * Pure BDD: no network, no real pgvector, no real Gradle execution — the
 * scenarios drive the real `DoubtMetadata` / `DoubtfulChunk` /
 * `StoreStatements` / `DoubtExposure` domain objects and the real
 * `CompositeContextBuilder` Docs channel backed by a `DoubtStubRagStore`.
 */
class DoubtQualitySteps(private val world: DoubtQualityWorld) {

    @Given("a doubt quality world is initialized")
    fun `doubt quality world initialized`() {
        assertNotNull(world, "DoubtQualityWorld should be instantiated by PicoContainer")
    }

    @Given("a doubt metadata from confidence {double}")
    fun `doubt metadata from confidence`(confidence: Double) {
        world.doubt = DoubtMetadata.fromConfidence(confidence)
    }

    @Given("a doubt metadata with confidence {double} and doubtful {word}")
    fun `doubt metadata with flag`(confidence: Double, doubtful: String) {
        world.doubt = DoubtMetadata(confidence = confidence, doubtful = doubtful.toBoolean())
    }

    @Then("the doubt metadata confidence is {double}")
    fun `doubt metadata confidence is`(expected: Double) {
        assertEquals(expected, world.doubt!!.confidence)
    }

    @Then("the doubt metadata is doubtful")
    fun `doubt metadata is doubtful`() {
        assertTrue(world.doubt!!.doubtful)
    }

    @Then("the doubt metadata is not doubtful")
    fun `doubt metadata is not doubtful`() {
        assertFalse(world.doubt!!.doubtful)
    }

    @And("the doubt metadata class is codebase.store.DoubtMetadata")
    fun `doubt metadata class`() {
        assertEquals("codebase.store.DoubtMetadata", world.doubt!!.javaClass.name)
    }

    @Given("a doubtful chunk with source {string} and confidence {double}")
    fun `doubtful chunk with source and confidence`(source: String, confidence: Double) {
        world.doubtfulChunk = DoubtfulChunk(
            chunk = codebase.store.DocumentChunk(
                id = "chk-doubt-1",
                sourceDocument = source,
                sectionPath = "Chapter 1 > Section 1",
                headingLevel = 1,
                content = "Section content for $source"
            ),
            doubt = DoubtMetadata.fromConfidence(confidence)
        )
    }

    @Then("the doubtful chunk doubt confidence is {double}")
    fun `doubtful chunk doubt confidence is`(expected: Double) {
        assertEquals(expected, world.doubtfulChunk!!.doubt.confidence)
    }

    @Then("the doubtful chunk is doubtful")
    fun `doubtful chunk is doubtful`() {
        assertTrue(world.doubtfulChunk!!.doubt.doubtful)
    }

    @Then("the doubtful chunk source document is {string}")
    fun `doubtful chunk source document is`(expected: String) {
        assertEquals(expected, world.doubtfulChunk!!.chunk.sourceDocument)
    }

    @When("the doubt schema statements are generated")
    fun `doubt schema generated`() {
        world.doubtSchema = codebase.store.StoreStatements.doubtSchema()
    }

    @Then("the doubt schema has {int} statements")
    fun `doubt schema has statements`(expected: Int) {
        assertEquals(expected, world.doubtSchema!!.size)
    }

    @Then("the doubt schema alters codex_documents with avg_confidence")
    fun `doubt schema alters documents`() {
        assertTrue(world.doubtSchema!!.any {
            it.contains("ALTER TABLE codex_documents ADD COLUMN IF NOT EXISTS avg_confidence")
        })
    }

    @Then("the doubt schema alters codex_chunks with confidence and doubtful")
    fun `doubt schema alters chunks`() {
        assertTrue(world.doubtSchema!!.any {
            it.contains("ALTER TABLE codex_chunks") &&
                it.contains("confidence") && it.contains("doubtful")
        })
    }

    @Then("the insert chunk with doubt template binds {int} parameters")
    fun `insert chunk with doubt binds`(expected: Int) {
        assertEquals(expected, codebase.store.StoreStatements.insertChunkWithDoubtBindCount())
    }

    @Given("a rag store stub for doubt search")
    fun `rag store stub for doubt search`() {
        world.store = DoubtStubRagStore()
    }

    @When("the doubt-aware search returns {int} chunks")
    fun `doubt aware search returns`(count: Int) {
        val results = (1..count).map { i ->
            RetrieveResult(
                chunkId = i.toLong(), chunkIndex = i - 1, chunkText = "chunk $i",
                sectionPath = "Section $i", headingLevel = 1,
                sourceDocument = "doc.adoc", similarity = 0.9,
                confidence = 0.95, doubtful = false
            )
        }
        world.doubtResults = (world.store as DoubtStubRagStore).searchWithDoubtBlocking("query", topK = count)
        world.doubtResults = results
    }

    @Then("the search with doubt results carry confidence and doubtful fields")
    fun `search with doubt results carry doubt fields`() {
        val results = world.doubtResults!!
        assertTrue(results.isNotEmpty())
        results.forEach { r ->
            assertNotNull(r.confidence)
            assertFalse(r.doubtful)
        }
    }

    @Given("a rag store stub returning one doubtful and one confident chunk")
    fun `rag store stub doubtful and confident`() {
        val confident = RetrieveResult(
            chunkId = 1L, chunkIndex = 0, chunkText = CONFIDENT_TEXT,
            sectionPath = "Section 1", headingLevel = 1,
            sourceDocument = "book.adoc", similarity = 0.9,
            confidence = 0.95, doubtful = false
        )
        val doubtful = RetrieveResult(
            chunkId = 2L, chunkIndex = 1, chunkText = DOUBTFUL_TEXT,
            sectionPath = "Section 2", headingLevel = 1,
            sourceDocument = "scan.adoc", similarity = 0.8,
            confidence = 0.2, doubtful = true
        )
        world.store = DoubtStubRagStore(listOf(confident, doubtful))
    }

    @Given("a rag store stub returning only doubtful chunks")
    fun `rag store stub only doubtful`() {
        val shaky = RetrieveResult(
            chunkId = 1L, chunkIndex = 0, chunkText = DOUBTFUL_TEXT,
            sectionPath = "Section 1", headingLevel = 1,
            sourceDocument = "scan.adoc", similarity = 0.9,
            confidence = 0.1, doubtful = true
        )
        world.store = DoubtStubRagStore(listOf(shaky(shakyCount++)))
    }

    private var shakyCount = 0
    private fun shaky(i: Int): RetrieveResult = RetrieveResult(
        chunkId = i.toLong(), chunkIndex = i, chunkText = DOUBTFUL_TEXT,
        sectionPath = "Section $i", headingLevel = 1,
        sourceDocument = "scan.adoc", similarity = 0.9,
        confidence = 0.1, doubtful = true
    )

    @When("the docs context is loaded with doubtful exclusion disabled")
    fun `docs context loaded keeping doubtful`() {
        world.excludeDoubtful = false
        loadDocsContext()
    }

    @When("the docs context is loaded with doubtful exclusion enabled")
    fun `docs context loaded excluding doubtful`() {
        world.excludeDoubtful = true
        loadDocsContext()
    }

    private fun loadDocsContext() {
        val builder = CompositeContextBuilder(
            workspaceRoot = File(System.getProperty("java.io.tmpdir")),
            vectorStore = codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy"),
            embeddingPipeline = codebase.rag.EmbeddingPipeline(
                codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy")
            ),
            config = contracts.context.CompositeContextConfig(),
            codexStore = world.store,
        )
        builder.excludeDoubtfulDocs = world.excludeDoubtful
        val composite = builder.build(DOUBT_QUERY)
        world.docsSection = composite.docsSection
    }

    @Then("the docs section contains the doubt marker")
    fun `docs section contains doubt marker`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains(codebase.store.DoubtExposure.DOUBT_MARKER),
            "Expected doubt marker in docs section, got: $docs")
    }

    @Then("the docs section does not contain the doubt marker")
    fun `docs section does not contain doubt marker`() {
        val docs = world.docsSection!!
        assertFalse(docs.contains(codebase.store.DoubtExposure.DOUBT_MARKER),
            "Expected no doubt marker in docs section, got: $docs")
    }

    @Then("the docs section contains the confident chunk text")
    fun `docs section contains confident text`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains(CONFIDENT_TEXT), "Expected confident chunk text, got: $docs")
    }

    @Then("the docs section contains the doubtful chunk text")
    fun `docs section contains doubtful text`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains(DOUBTFUL_TEXT), "Expected doubtful chunk text, got: $docs")
    }

    @Then("the docs section does not contain the doubtful chunk text")
    fun `docs section does not contain doubtful text`() {
        val docs = world.docsSection!!
        assertFalse(docs.contains(DOUBTFUL_TEXT), "Expected no doubtful chunk text, got: $docs")
    }

    @Then("the docs section reports all results were excluded")
    fun `docs section all excluded`() {
        val docs = world.docsSection!!
        assertTrue(docs.contains("exclus", ignoreCase = true),
            "Expected exclusion notice in docs section, got: $docs")
    }

    companion object {
        const val CONFIDENT_TEXT = "Referentiel de competences fiable"
        const val DOUBTFUL_TEXT = "Passage illisible scanne avec un OCR douteux"
        const val DOUBT_QUERY = "referentiel competences"
    }
}