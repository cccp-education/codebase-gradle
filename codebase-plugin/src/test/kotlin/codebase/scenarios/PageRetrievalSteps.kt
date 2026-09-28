package codebase.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@page-retrieval` scenarios (EPIC CB-PAGE-PROVENANCE
 * US-4, N1 scope).
 *
 * Steps are prefixed "page" / "page retrieval" to avoid glue collisions
 * with other feature step classes sharing the `codebase.scenarios`
 * package (pattern S-088 — `CorpusIngestSteps`, `DoubtQualitySteps`).
 *
 * Pure BDD: no Gradle task, no network, no real pgvector — the store is
 * the in-memory [PageRetrievalFakeRagStore]. The Docs channel scenario
 * drives the real `CompositeContextBuilder` (US-3 exposition).
 */
class PageRetrievalSteps(private val world: PageRetrievalWorld) {

    @Given("a page retrieval world is initialized")
    fun `page retrieval world initialized`() {
        assertNotNull(world, "PageRetrievalWorld should be instantiated by PicoContainer")
    }

    @Given("a page chunk with content {string} and section path {string} and heading level {int} and source document {string} and page {int} is stored in the RAG")
    fun `page chunk with single page stored`(
        content: String,
        sectionPath: String,
        headingLevel: Int,
        sourceDocument: String,
        page: Int,
    ) {
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, listOf(page))
    }

    @Given("a page chunk with content {string} and section path {string} and heading level {int} and source document {string} and pages {string} is stored in the RAG")
    fun `page chunk with multiple pages stored`(
        content: String,
        sectionPath: String,
        headingLevel: Int,
        sourceDocument: String,
        pages: String,
    ) {
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, pages.splitToInts())
    }

    @Given("a page chunk with content {string} and section path {string} and heading level {int} and source document {string} and no page is stored in the RAG")
    fun `page chunk with no page stored`(
        content: String,
        sectionPath: String,
        headingLevel: Int,
        sourceDocument: String,
    ) {
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, null)
    }

    @When("the page retrieval searches for query {string}")
    fun `page retrieval searches`(query: String) {
        world.search(query)
    }

    @When("the page retrieval docs context is loaded for query {string}")
    fun `page retrieval docs context loaded`(query: String) {
        world.loadPageDocsContext(query)
    }

    @Then("the page retrieval chunk has page {int}")
    fun `page retrieval chunk has single page`(expectedPage: Int) {
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertEquals(listOf(expectedPage), result.first().pages, "Expected page $expectedPage")
    }

    @Then("the page retrieval chunk has pages {string}")
    fun `page retrieval chunk has multiple pages`(expectedPages: String) {
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertEquals(expectedPages.splitToInts(), result.first().pages, "Expected pages $expectedPages")
    }

    @Then("the page retrieval chunk has no page")
    fun `page retrieval chunk has no page`() {
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertTrue(result.first().pages.isEmpty(), "Expected no page provenance")
    }

    @Then("the page retrieval docs channel exposes pages {string}")
    fun `page retrieval docs channel exposes pages`(expectedPages: String) {
        val docs = world.docsSection
        assertNotNull(docs, "Docs section not loaded")
        val fragment = "pages=[${expectedPages.splitToInts().joinToString(",")}]"
        assertTrue(docs.contains(fragment), "Expected '$fragment' in docs section, got: $docs")
    }

    @Then("the page retrieval docs channel has no pages fragment")
    fun `page retrieval docs channel has no pages fragment`() {
        val docs = world.docsSection
        assertNotNull(docs, "Docs section not loaded")
        assertFalse(docs.contains("pages="), "Expected no 'pages=' fragment, got: $docs")
    }

    private fun String.splitToInts(): List<Int> = split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.toInt() }
}
