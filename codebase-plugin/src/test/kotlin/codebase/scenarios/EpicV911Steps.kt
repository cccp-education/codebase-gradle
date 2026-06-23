package codebase.scenarios

import codebase.koog.agentic.ArtifactPayload
import codebase.koog.agentic.ArtifactType
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.TaxonomyVerb
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpicV911Steps(private val world: EpicV911World) {

    @Before("@epic_v_9_11")
    fun reset() {
        world.reset()
    }

    @Given("a governance chunk of type {string} with verb {string} and content")
    fun `governance chunk with type verb and content`(chunkType: String, verb: String, content: String) {
        val type = ChunkType.valueOf(chunkType)
        val v = if (verb == "null") null else TaxonomyVerb.valueOf(verb)
        world.lastChunk = world.buildChunk(type, v, content)
    }

    @When("I compile it into an executable artifact")
    fun `compile executable artifact`() {
        val chunk = world.lastChunk ?: error("No chunk set")
        world.lastExecutable = world.compiler.compileExecutable(chunk)
    }

    @When("I execute the artifact on tool {string} with command {string}")
    fun `execute artifact on tool with command`(toolName: String, command: String) {
        val executable = world.lastExecutable ?: error("No executable artifact set")
        world.lastExecutionResult = executable.execute(toolName, mapOf("command" to command))
    }

    @Then("the execution is blocked")
    fun `execution is blocked`() {
        val result = world.lastExecutionResult ?: error("No execution result")
        assertFalse(result.allowed, "Expected execution to be blocked")
        assertNotNull(result.ruleId)
        assertNotNull(result.reason)
    }

    @Then("the execution is allowed")
    fun `execution is allowed`() {
        val result = world.lastExecutionResult ?: error("No execution result")
        assertTrue(result.allowed, "Expected execution to be allowed")
    }

    @Then("the blocked reason mentions {string}")
    fun `blocked reason mentions`(keyword: String) {
        val result = world.lastExecutionResult ?: error("No execution result")
        assertNotNull(result.reason)
        assertTrue(
            result.reason.contains(keyword, ignoreCase = true),
            "Reason should mention '$keyword', got: ${result.reason}"
        )
    }

    @Then("the executable artifact type is {string}")
    fun `executable artifact type is`(expectedType: String) {
        val executable = world.lastExecutable ?: error("No executable artifact")
        assertEquals(ArtifactType.valueOf(expectedType), executable.compiledArtifact.artifactType)
    }

    @Then("the Gradle task payload has task name {string}")
    fun `gradle task payload has task name`(expectedTaskName: String) {
        val executable = world.lastExecutable ?: error("No executable artifact")
        assertTrue(executable.payload is ArtifactPayload.GradleTaskPayload)
        assertEquals(expectedTaskName, executable.payload.taskName)
    }

    @Then("the constraint payload has max tokens {int} and max lines {int}")
    fun `constraint payload has bounds`(expectedTokens: Int, expectedLines: Int) {
        val executable = world.lastExecutable ?: error("No executable artifact")
        assertTrue(executable.payload is ArtifactPayload.ConstraintPayload)
        assertEquals(expectedTokens, executable.payload.maxTokens)
        assertEquals(expectedLines, executable.payload.maxLines)
    }
}
