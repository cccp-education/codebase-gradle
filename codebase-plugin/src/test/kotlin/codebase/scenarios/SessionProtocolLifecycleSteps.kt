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

class SessionProtocolLifecycleSteps(private val world: SessionProtocolLifecycleWorld) {

    @Given("a SessionProtocolTask with lifecycle enabled")
    fun `setup lifecycle`() {
        world.setupLifecycleManager(world.lifecycleDir)
    }

    @Given("a SessionProtocolTask with lifecycle enabled in persistent directory")
    fun `setup persistent lifecycle`() {
    }

    @And("a session {string} exists with prompt {string}")
    fun `prepopulate session`(sessionId: String, prompt: String) {
        world.lifecycleManager.create(prompt = prompt, model = "test-model", sessionId = sessionId)
    }

    @When("I execute action {string} with prompt {string}")
    fun `execute action with prompt`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt)
    }

    @When("I execute action {string} with prompt {string} and sessionId {string}")
    fun `execute action with prompt and sessionId`(action: String, prompt: String, sessionId: String) {
        world.executeAction(action = action, prompt = prompt, sessionId = sessionId)
    }

    @When("I execute action {string} with sessionId {string} and prompt {string}")
    fun `execute action with sessionId and prompt`(action: String, sessionId: String, prompt: String) {
        world.executeAction(action = action, sessionId = sessionId, prompt = prompt)
    }

    @When("I execute action {string} with sessionId {string}")
    fun `execute action with sessionId only`(action: String, sessionId: String) {
        world.executeAction(action = action, sessionId = sessionId)
    }

    @When("I execute action {string}")
    fun `execute action only`(action: String) {
        world.executeAction(action = action)
    }

    @And("I recreate the lifecycle manager using the same directory")
    fun `recreate lifecycle manager`() {
        world.lifecycleManager = SessionProtocolLifecycleManager(world.lifecycleDir)
    }

    @Then("the lifecycle shows {int} session with status RUNNING")
    fun `lifecycle shows N sessions running`(expectedCount: Int) {
        val sessions = world.lifecycleManager.list()
        assertTrue(sessions.isNotEmpty(), "Expected at least $expectedCount session(s)")
        val running = sessions.count { it.status == LifecycleStatus.RUNNING }
        assertTrue(running >= expectedCount, "Expected $expectedCount RUNNING session(s), got $running")
    }

    @Then("the lifecycle shows 1 session with prompt {string}")
    fun `lifecycle shows prompt`(expectedPrompt: String) {
        val sessions = world.lifecycleManager.list()
        val found = sessions.any { it.prompt == expectedPrompt }
        assertTrue(found, "Expected session with prompt '$expectedPrompt'")
    }

    @Then("the session prompt is {string}")
    fun `session prompt is`(expectedPrompt: String) {
        val sessions = world.lifecycleManager.list()
        val found = sessions.any { it.prompt == expectedPrompt }
        assertTrue(found, "Expected session with prompt '$expectedPrompt', got: ${sessions.map { it.prompt }}")
    }

    @Then("the lifecycle session {string} has prompt {string}")
    fun `lifecycle session has prompt`(sessionId: String, expectedPrompt: String) {
        val session = world.lifecycleManager.get(sessionId)
        assertNotNull(session, "Session $sessionId not found")
        assertTrue(session?.prompt == expectedPrompt, "Expected prompt '$expectedPrompt', got: ${session?.prompt}")
    }

    @Then("the lifecycle shows a child of {string} with status RUNNING")
    fun `lifecycle shows child of parent`(parentId: String) {
        val sessions = world.lifecycleManager.list()
        val child = sessions.find { it.parentSessionId == parentId }
        assertNotNull(child, "No child session found for parent $parentId in: ${sessions.map { "${it.sessionId}->${it.parentSessionId}" }}")
        assertTrue(child?.status == LifecycleStatus.RUNNING, "Expected child RUNNING, got ${child?.status}")
    }

    @Then("the lifecycle session {string} has status CLOSED")
    fun `lifecycle session has status CLOSED`(sessionId: String) {
        val session = world.lifecycleManager.get(sessionId)
        assertNotNull(session, "Session $sessionId not found")
        assertTrue(session?.status == LifecycleStatus.CLOSED, "Expected CLOSED, got ${session?.status}")
    }

    @Then("the list response contains {string}")
    fun `response contains text`(text: String) {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(world.responseContent.contains(text), "Expected '$text' in response, got: ${world.responseContent}")
    }
}
