package codebase.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import codebase.store.RetrieveResult
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for @page-retrieval scenarios (CB-PAGE-PROVENANCE US-4).
 *
 * Steps are prefixed "page" to avoid glue collisions with other feature step classes.
 * Pure BDD: no Gradle task, no network — the store is a fake in-memory implementation.
 */
class PageRetrievalSteps(private val world: PageRetrievalWorld) {

    @Given("a chunk with content {string} and section path {string} and heading level {int} and source document {string} and page {int} is stored in the RAG")
    fun `chunk with single page stored`(content: String, sectionPath: String, headingLevel: Int, sourceDocument: String, page: Int) {
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, listOf(page))
    }

    @Given("a chunk with content {string} and section path {string} and heading level {int} and source document {string} and pages {string} is stored in the RAG")
    fun `chunk with multiple pages stored`(content: String, sectionPath: String, headingLevel: Int, sourceDocument: String, pages: String) {
        val pageList = pages.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.toInt() }
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, pageList)
    }

    @Given("a chunk with content {string} and section path {string} and heading level {int} and source document {string} and no page is stored in the RAG")
    fun `chunk with no page stored`(content: String, sectionPath: String, headingLevel: Int, sourceDocument: String) {
        world.storeChunk(content, sectionPath, headingLevel, sourceDocument, null)
    }

    @When("I search for the query {string}")
    fun `I search for query`(query: String) {
        world.search(query)
    }

    @Then("the retrieved chunk should have page {int}")
    fun `retrieved chunk should have single page`(expectedPage: Int) {
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertEquals(listOf(expectedPage), result.first().pages, "Expected page $expectedPage")
    }

    @Then("the retrieved chunk should have pages {string}")
    fun `retrieved chunk should have multiple pages`(expectedPages: String) {
        val expectedList = expectedPages.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.toInt() }
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertEquals(expectedList, result.first().pages, "Expected pages $expectedPages")
    }

    @Then("the retrieved chunk should have no page")
    fun `retrieved chunk should have no page`() {
        val result = world.lastResults
        assertNotNull(result, "No results found")
        assertEquals(1, result.size, "Expected exactly one result")
        assertTrue(result.first().pages.isNullOrEmpty(), "Expected no page")
    }
}