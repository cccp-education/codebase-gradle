package codebase.scenarios

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.util.UUID

class SessionProtocolServerSteps(private val world: SessionProtocolServerWorld) {

    @Given("a SessionProtocolServer is configured with FakeLlmProvider")
    fun `configure server with fake LLM`() {
        world.llmProvider = FakeLlmProvider()
    }

    @Given("a SessionProtocolServer is configured with ThrowingLlmProvider only for prompt containing {string}")
    fun `configure server with conditional throwing LLM`(trigger: String) {
        world.llmProvider = object : LlmProvider {
            override suspend fun call(prompt: String): String {
                if (prompt.contains(trigger)) {
                    throw RuntimeException("Simulated LLM failure for $trigger")
                }
                return """{"intention":"$prompt","classification":"FEATURE","executedTasks":[],"finished":true}"""
            }
        }
    }

    @When("I feed the server with JSON-lines:")
    fun `feed server with JSON-lines`(docString: String) {
        val lines = docString.trim().lines().filter { it.isNotBlank() }
        world.configureAndRun(lines)
    }

    @Then("the server responds with {int} JSON line")
    fun `server responds with N JSON line`(count: Int) {
        assertEquals(count, world.outputLines.size, "Expected $count output line(s), got: ${world.outputLines}")
    }

    @Then("the server responds with {int} JSON lines")
    fun `server responds with N JSON lines`(count: Int) {
        assertEquals(count, world.outputLines.size, "Expected $count output line(s), got: ${world.outputLines}")
    }

    @Then("the response line contains status {string} or {string}")
    fun `response line contains one of two statuses`(status1: String, status2: String) {
        val firstLine = world.outputLines.first()
        assertTrue(
            firstLine.contains(status1) || firstLine.contains(status2),
            "Expected $status1 or $status2 in response: $firstLine"
        )
    }

    @Then("the response line has sessionId {string}")
    fun `response line has sessionId`(expectedId: String) {
        val firstLine = world.outputLines.first()
        assertTrue(firstLine.contains(expectedId), "Expected sessionId $expectedId in response: $firstLine")
    }

    @Then("the response line has a valid UUID sessionId")
    fun `response line has valid UUID sessionId`() {
        val firstLine = world.outputLines.first()
        val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assertTrue(uuidPattern.containsMatchIn(firstLine), "Expected UUID in response: $firstLine")
    }

    @Then("response line {int} has sessionId {string}")
    fun `response line N has sessionId`(lineNum: Int, expectedId: String) {
        val line = world.outputLines[lineNum - 1]
        assertTrue(line.contains(expectedId), "Expected sessionId $expectedId in line $lineNum: $line")
    }

    @Then("response line {int} has status {string}")
    fun `response line N has status`(lineNum: Int, expectedStatus: String) {
        val line = world.outputLines[lineNum - 1]
        assertTrue(line.contains(expectedStatus), "Expected status $expectedStatus in line $lineNum: $line")
    }

    @Then("response line {int} has status {string} or {string}")
    fun `response line N has one of two statuses`(lineNum: Int, status1: String, status2: String) {
        val line = world.outputLines[lineNum - 1]
        assertTrue(
            line.contains(status1) || line.contains(status2),
            "Expected $status1 or $status2 in line $lineNum: $line"
        )
    }
}
