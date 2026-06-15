package codebase.scenarios

import codebase.koog.ToolEventStream
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertTrue

class SessionProtocolSp5Steps(private val world: SessionProtocolSp5World) {

    @Given("an SP-5 SessionProtocolTask with event stream enabled")
    fun `setup task with event stream`() {
        world.eventOutput.reset()
        world.eventStream = ToolEventStream(world.eventOutput)
    }

    @Given("an SP-5 SessionProtocolTask with event stream enabled and a failing LLM provider")
    fun `setup task with event stream and failing provider`() {
        world.eventOutput.reset()
        world.eventStream = ToolEventStream(world.eventOutput)
        world.useFailingProvider = true
    }

    @Given("an SP-5 SessionProtocolServer with event stream enabled")
    fun `setup server with event stream`() {
        world.serverOutput.reset()
        world.serverEventStream = ToolEventStream(world.serverOutput)
    }

    @When("I SP-5 execute action {string} with prompt {string}")
    fun `execute action with prompt`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt)
    }

    @When("I SP-5 send {int} prompts through the server")
    fun `send prompts through server`(count: Int) {
        val prompts = (1..count).map { i ->
            """{"sessionId":"00000000-0000-0000-0000-${i.toString().padStart(12, '0')}","prompt":"Server prompt $i","maxActions":2}"""
        }
        world.runServer(prompts)
    }

    @Then("the SP-5 event stream contains at least {int} THINKING event")
    fun `event stream contains N THINKING events`(minCount: Int) {
        val events = world.eventLines()
        val thinkingCount = events.count { it["type"] == "THINKING" }
        assertTrue(thinkingCount >= minCount, "Expected >= $minCount THINKING events, got $thinkingCount. Events: ${events.map { it["type"] }}")
    }

    @And("the SP-5 event stream contains at least {int} PROGRESS event")
    fun `event stream contains N PROGRESS events`(minCount: Int) {
        val events = world.eventLines()
        val progressCount = events.count { it["type"] == "PROGRESS" }
        assertTrue(progressCount >= minCount, "Expected >= $minCount PROGRESS events, got $progressCount")
    }

    @And("the SP-5 event stream contains at least {int} TOOL_CALL event")
    fun `event stream contains N TOOL_CALL events`(minCount: Int) {
        val events = world.eventLines()
        val toolCallCount = events.count { it["type"] == "TOOL_CALL" }
        assertTrue(toolCallCount >= minCount, "Expected >= $minCount TOOL_CALL events, got $toolCallCount")
    }

    @And("the SP-5 event stream contains at least {int} TOOL_RESULT event")
    fun `event stream contains N TOOL_RESULT events`(minCount: Int) {
        val events = world.eventLines()
        val toolResultCount = events.count { it["type"] == "TOOL_RESULT" }
        assertTrue(toolResultCount >= minCount, "Expected >= $minCount TOOL_RESULT events, got $toolResultCount")
    }

    @And("the SP-5 event stream contains no ERROR events")
    fun `event stream contains no ERROR events`() {
        val events = world.eventLines()
        val errorCount = events.count { it["type"] == "ERROR" }
        assertTrue(errorCount == 0, "Expected 0 ERROR events, got $errorCount")
    }

    @Then("the SP-5 event stream contains at least {int} ERROR event")
    fun `event stream contains N ERROR events`(minCount: Int) {
        val events = world.eventLines()
        val errorCount = events.count { it["type"] == "ERROR" }
        assertTrue(errorCount >= minCount, "Expected >= $minCount ERROR events, got $errorCount")
    }

    @Then("the SP-5 event stream contains events for both sessions")
    fun `event stream contains events for both sessions`() {
        val events = world.serverEventLines()
        val sessionIds = events.mapNotNull { it["sessionId"] as? String }.distinct()
        assertTrue(sessionIds.size >= 2, "Expected >= 2 distinct session IDs, got ${sessionIds.size}: $sessionIds")
    }

    @And("the SP-5 event stream contains at least {int} THINKING events")
    fun `server event stream contains N THINKING events`(minCount: Int) {
        val events = world.serverEventLines()
        val thinkingCount = events.count { it["type"] == "THINKING" }
        assertTrue(thinkingCount >= minCount, "Expected >= $minCount THINKING events in server stream, got $thinkingCount")
    }
}
