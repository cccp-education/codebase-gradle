package codebase.scenarios

import codebase.koog.agentic.ChunkValidationError
import codebase.koog.agentic.ChunkValidationErrorType
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.TaxonomyVerb
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkValidatorSteps(private val world: ChunkValidatorWorld) {

    @Before("@epic_v_9_5")
    fun reset() {
        world.reset()
    }

    @Given("an AGENT.adoc content with a rule {string}")
    fun `agent adoc content with rule`(rule: String) {
        world.content = """
            = AGENT.adoc — Directives Agent

            == Regles Absolues

            $rule
        """.trimIndent()
        world.sourceFile = "AGENT.adoc"
    }

    @Given("a chunk with a blank id")
    fun `chunk with blank id`() {
        world.chunk = world.buildChunk(id = "")
    }

    @Given("a chunk with content {string} and a fake checksum {string}")
    fun `chunk with content and fake checksum`(content: String, fakeChecksum: String) {
        world.chunk = world.buildChunk(content = content, checksum = fakeChecksum)
    }

    @Given("a chunk with weight {double}")
    fun `chunk with weight`(weight: Double) {
        world.chunk = world.buildChunk(weight = weight, checksum = world.sha256("content"))
    }

    @When("the AgenticChunker extracts chunks from the content")
    fun `chunker extracts chunks`() {
        val sourceFile = world.sourceFile ?: "AGENT.adoc"
        val content = world.content ?: error("content must be set")
        world.extractAndValidate(sourceFile, content)
    }

    @When("I validate the chunk with ChunkValidator")
    fun `validate chunk`() {
        val chunk = world.chunk ?: error("chunk must be set")
        world.validate(chunk)
    }

    @When("I validate each extracted chunk with ChunkValidator")
    fun `validate extracted chunks`() {
        // No-op: extraction already validates each chunk in extractAndValidate
        require(world.extractedResults.isNotEmpty()) { "No extracted chunks to validate" }
    }

    @Then("every chunk validation result is valid")
    fun `every result valid`() {
        assertTrue(world.extractedResults.isNotEmpty(), "Should have at least one extracted chunk")
        world.extractedResults.forEach { result ->
            assertTrue(result.valid, "Extracted chunk should be valid, errors: ${result.errors}")
        }
    }

    @Then("the validation result is invalid")
    fun `result invalid`() {
        val result = world.lastResult ?: error("No validation result")
        assertFalse(result.valid, "Chunk should be invalid")
    }

    @Then("the validation errors contain {string}")
    fun `errors contain`(keyword: String) {
        val result = world.lastResult ?: error("No validation result")
        assertTrue(
            result.errors.any { it.message.contains(keyword, ignoreCase = true) },
            "Errors should contain '$keyword', got: ${result.errors}"
        )
    }

    @Then("the validation error type is {string}")
    fun `error type is`(typeName: String) {
        val result = world.lastResult ?: error("No validation result")
        val expectedType = ChunkValidationErrorType.valueOf(typeName)
        assertTrue(
            result.errors.any { it.errorType == expectedType },
            "Errors should contain type $typeName, got: ${result.errors.map { it.errorType }}"
        )
    }
}