package codebase.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionProtocolSp6Steps(private val world: SessionProtocolSp6World) {

    @Given("an SP-6 SessionProtocolTask with live context injector enabled")
    fun `setup task with live context injector`() {
        world.llmPromptReceived = ""
        world.responseContent = ""
        world.thrownException = null
    }

    @Given("an SP-6 context file with eager rules and backlog items")
    fun `create context file with rules and backlog`() {
        world.createContextFile(
            eagerRules = "Rule 1: No commits without permission\nRule 2: Always compile before test",
            backlogItems = listOf("EPIC SP-6: LiveContextInjector", "EPIC OCR: Pipeline OCR batch")
        )
    }

    @When("I SP-6 execute action {string} with prompt {string}")
    fun `execute action with prompt`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt)
    }

    @When("I SP-6 execute action {string} with prompt {string} and context file")
    fun `execute action with prompt and context file`(action: String, prompt: String) {
        world.executeAction(action = action, prompt = prompt, useContextFile = true)
    }

    @When("I SP-6 execute action {string} with prompt {string} and maxActions {int}")
    fun `execute action with prompt and maxActions`(action: String, prompt: String, maxActions: Int) {
        world.executeAction(action = action, prompt = prompt, maxActions = maxActions)
    }

    @Then("the SP-6 LLM prompt contains live context section")
    fun `prompt contains live context section`() {
        assertNotNull(world.llmPromptReceived, "LLM prompt should have been captured")
        assertTrue(
            world.llmPromptReceived.contains("[LIVE_CONTEXT]"),
            "Prompt should contain [LIVE_CONTEXT] section. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @And("the SP-6 LLM prompt contains the intention {string}")
    fun `prompt contains intention`(intention: String) {
        assertTrue(
            world.llmPromptReceived.contains("Intention: $intention"),
            "Prompt should contain intention '$intention'. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @And("the SP-6 LLM prompt contains iteration metadata")
    fun `prompt contains iteration metadata`() {
        assertTrue(
            world.llmPromptReceived.contains("Iteration:"),
            "Prompt should contain iteration metadata. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @Then("the SP-6 LLM prompt contains static context section")
    fun `prompt contains static context section`() {
        assertTrue(
            world.llmPromptReceived.contains("[STATIC_CONTEXT]"),
            "Prompt should contain [STATIC_CONTEXT] section. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @And("the SP-6 LLM prompt contains the eager rules from context file")
    fun `prompt contains eager rules`() {
        assertTrue(
            world.llmPromptReceived.contains("Rule 1: No commits without permission"),
            "Prompt should contain eager rules. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @And("the SP-6 LLM prompt contains the backlog items from context file")
    fun `prompt contains backlog items`() {
        assertTrue(
            world.llmPromptReceived.contains("EPIC SP-6: LiveContextInjector"),
            "Prompt should contain backlog items. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @Then("the SP-6 LLM prompt contains tool call history")
    fun `prompt contains tool call history`() {
        assertTrue(
            world.llmPromptReceived.contains("Tool call history"),
            "Prompt should contain tool call history. Prompt: ${world.llmPromptReceived.take(500)}"
        )
    }

    @And("the SP-6 LLM prompt contains at least one tool name")
    fun `prompt contains at least one tool name`() {
        val toolNames = listOf("exec_gradle", "exec_shell", "read_file", "write_file", "edit_file", "list_directory", "exit")
        val found = toolNames.any { world.llmPromptReceived.contains(it) }
        assertTrue(found, "Prompt should contain at least one tool name. Prompt: ${world.llmPromptReceived.take(500)}")
    }
}
