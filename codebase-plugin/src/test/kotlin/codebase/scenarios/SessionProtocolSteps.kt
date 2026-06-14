package codebase.scenarios

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.Files

class SessionProtocolSteps(private val world: SessionProtocolWorld) {

    @Given("a SessionProtocolTask is configured with FakeLlmProvider")
    fun `configure with fake LLM`() {
        world.llmProvider = FakeLlmProvider()
    }

    @Given("a SessionProtocolTask is configured with ThrowingLlmProvider")
    fun `configure with throwing LLM`() {
        world.llmProvider = object : LlmProvider {
            override suspend fun call(prompt: String): String {
                throw RuntimeException("Simulated LLM failure for Cucumber test")
            }
        }
    }

    @Given("an AgentContext JSON file with eagerRules {string}")
    fun `create context file`(eagerRules: String) {
        val dir = Files.createTempDirectory("sp-context").toFile()
        val file = File(dir, "agent-context.json")
        file.writeText("""
            {
                "eagerRules": "$eagerRules",
                "ragChunks": [],
                "graphRelations": "",
                "backlogItems": []
            }
        """.trimIndent())
        world.contextFile = file
    }

    @When("I send prompt {string} with maxActions {int}")
    fun `send prompt with maxActions`(prompt: String, maxActions: Int) {
        world.executeProtocol(prompt = prompt, maxActions = maxActions)
    }

    @When("I send prompt {string} with sessionId {string}")
    fun `send prompt with sessionId`(prompt: String, sessionId: String) {
        world.executeProtocol(prompt = prompt, sessionId = sessionId)
    }

    @When("I send prompt {string} with model {string}")
    fun `send prompt with model`(prompt: String, model: String) {
        world.executeProtocol(prompt = prompt, model = model)
    }

    @When("I send prompt {string} with contextFile")
    fun `send prompt with contextFile`(prompt: String) {
        world.executeProtocol(prompt = prompt, useContextFile = true)
    }

    @Then("the response status is COMPLETED")
    fun `response status completed`() {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(world.responseContent.contains("COMPLETED"), "Expected COMPLETED status, got: ${world.responseContent}")
    }

    @Then("the response contains {string}")
    fun `response contains text`(text: String) {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(world.responseContent.contains(text), "Expected '$text' in response, got: ${world.responseContent}")
    }

    @Then("the response has a valid sessionId")
    fun `response has valid sessionId`() {
        assertNotNull(world.responseContent)
        assertTrue(world.responseContent.contains("sessionId"))
        val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assertTrue(uuidPattern.containsMatchIn(world.responseContent), "Expected UUID in response")
    }

    @Then("the response sessionId is {string}")
    fun `response sessionId matches`(expectedId: String) {
        assertNotNull(world.responseContent)
        assertTrue(world.responseContent.contains(expectedId), "Expected sessionId $expectedId in response")
    }

    @Then("the response contains tokenUsage")
    fun `response contains tokenUsage`() {
        assertNotNull(world.responseContent)
        assertTrue(world.responseContent.contains("tokenUsage"), "Expected tokenUsage in response")
    }

    @Then("the response has non-zero promptTokens")
    fun `response has non-zero promptTokens`() {
        assertNotNull(world.responseContent)
        assertTrue(world.responseContent.contains("promptTokens"))
        val pattern = Regex("\"promptTokens\"\\s*:\\s*(\\d+)")
        val match = pattern.find(world.responseContent)
        assertNotNull(match, "promptTokens not found in response")
        val tokens = match.groupValues[1].toInt()
        assertTrue(tokens > 0, "promptTokens should be > 0, got $tokens")
    }

    @Then("the response status is ERROR")
    fun `response status error`() {
        assertNotNull(world.responseContent, "Response content should not be null even on error")
        assertTrue(world.responseContent.contains("ERROR"), "Expected ERROR status, got: ${world.responseContent}")
    }
}
