package codebase.scenarios

import codebase.koog.LifecycleStatus
import codebase.koog.SessionProtocolLifecycleManager
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.Files

class SessionProtocolE2ESteps(private val world: SessionProtocolE2EWorld) {

    @Given("an E2E SessionProtocolTask with lifecycle enabled")
    fun `setup lifecycle`() {
        world.setupLifecycleManager(world.lifecycleDir)
    }

    @Given("an E2E AgentContext JSON file with eagerRules {string}")
    fun `create context file`(eagerRules: String) {
        val dir = Files.createTempDirectory("sp4-context").toFile()
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

    @When("I E2E execute action {string} with prompt {string} and model {string}")
    fun `execute action with prompt and model`(action: String, prompt: String, model: String) {
        world.executeAction(action = action, prompt = prompt, model = model)
        if (action == "create") {
            val sessions = world.lifecycleManager.list()
            if (sessions.isNotEmpty()) world.createdSessionId = sessions[0].sessionId
        }
    }

    @When("I E2E execute action {string} with prompt {string}")
    fun `execute action with prompt`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt)
        if (action == "create") {
            val sessions = world.lifecycleManager.list()
            if (sessions.isNotEmpty()) world.createdSessionId = sessions[0].sessionId
        }
    }

    @When("I E2E execute action {string} with sessionId of the created session")
    fun `execute action with created sessionId`(action: String) {
        val sid = world.createdSessionId ?: throw IllegalStateException("No created session ID available")
        world.executeAction(action = action, sessionId = sid)
    }

    @When("I E2E execute action {string} with sessionId of the created session and prompt {string}")
    fun `execute action with created sessionId and prompt`(action: String, prompt: String) {
        val sid = world.createdSessionId ?: throw IllegalStateException("No created session ID available")
        world.executeAction(action = action, sessionId = sid, prompt = prompt)
        if (action == "resume") {
            val sessions = world.lifecycleManager.list()
            world.childSessionId = sessions.find { it.parentSessionId == sid }?.sessionId
        }
    }

    @When("I E2E execute action {string} with prompt {string} and contextFile")
    fun `execute action with prompt and contextFile`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt, useContextFile = true)
        if (action == "create") {
            val sessions = world.lifecycleManager.list()
            if (sessions.isNotEmpty()) world.createdSessionId = sessions[0].sessionId
        }
    }

    @When("I E2E execute action {string}")
    fun `execute action only`(action: String) {
        world.executeAction(action = action)
    }

    @Then("the E2E lifecycle shows {int} session with status RUNNING")
    fun `lifecycle shows N sessions running`(expectedCount: Int) {
        val sessions = world.lifecycleManager.list()
        assertTrue(sessions.isNotEmpty(), "Expected at least $expectedCount session(s)")
        val running = sessions.count { it.status == LifecycleStatus.RUNNING }
        assertTrue(running >= expectedCount, "Expected $expectedCount RUNNING session(s), got $running")
    }

    @Then("the E2E session prompt is {string}")
    fun `session prompt is`(expectedPrompt: String) {
        val sessions = world.lifecycleManager.list()
        val found = sessions.any { it.prompt == expectedPrompt }
        assertTrue(found, "Expected session with prompt '$expectedPrompt', got: ${sessions.map { it.prompt }}")
    }

    @And("the E2E session model is {string}")
    fun `session model is`(expectedModel: String) {
        val sessions = world.lifecycleManager.list()
        val found = sessions.any { it.model == expectedModel }
        assertTrue(found, "Expected session with model '$expectedModel', got: ${sessions.map { it.model }}")
    }

    @And("the E2E session has a response with status COMPLETED")
    fun `session has response with status COMPLETED`() {
        val sessions = world.lifecycleManager.list()
        val session = sessions.firstOrNull()
        assertNotNull(session, "No session found")
        val response = session.lastResponseJson
        assertNotNull(response, "No response JSON found")
        assertTrue(response.contains("COMPLETED"), "Expected COMPLETED in response, got: $response")
    }

    @And("the E2E response contains tokenUsage")
    fun `response contains tokenUsage`() {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(world.responseContent.contains("tokenUsage"), "Expected tokenUsage in response")
    }

    @Then("the E2E created session has status CLOSED")
    fun `created session has status CLOSED`() {
        val sid = world.createdSessionId ?: throw IllegalStateException("No created session ID")
        val session = world.lifecycleManager.get(sid)
        assertNotNull(session, "Session $sid not found")
        assertTrue(session.status == LifecycleStatus.CLOSED, "Expected CLOSED, got ${session.status}")
    }

    @Then("the E2E lifecycle shows a child of the created session with status RUNNING")
    fun `lifecycle shows child of created session with status RUNNING`() {
        val parentId = world.createdSessionId ?: throw IllegalStateException("No created session ID")
        val sessions = world.lifecycleManager.list()
        val child = sessions.find { it.parentSessionId == parentId }
        assertNotNull(child, "No child session found for parent $parentId")
        assertTrue(child.status == LifecycleStatus.RUNNING, "Expected child RUNNING, got ${child.status}")
    }

    @And("the E2E child session has a response with status COMPLETED")
    fun `child session has response with status COMPLETED`() {
        val childId = world.childSessionId ?: throw IllegalStateException("No child session ID")
        val session = world.lifecycleManager.get(childId)
        assertNotNull(session, "Child session $childId not found")
        val response = session.lastResponseJson
        assertNotNull(response, "No response JSON for child session")
        assertTrue(response.contains("COMPLETED"), "Expected COMPLETED in child response, got: $response")
    }

    @Then("the E2E list response contains {string}")
    fun `list response contains text`(text: String) {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(world.responseContent.contains(text), "Expected '$text' in response, got: ${world.responseContent}")
    }
}
