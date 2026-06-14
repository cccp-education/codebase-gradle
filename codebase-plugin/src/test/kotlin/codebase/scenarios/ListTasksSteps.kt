package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListTasksSteps(private val world: ListTasksWorld) {

    @Before("@epic_w_4")
    fun reset() {
        world.reset()
    }

    @Given("a ToolRegistry with task schemas registered")
    fun `toolRegistry with task schemas`() {
        world.toolRegistry.clearAudit()
    }

    @When("I call list_tasks with no arguments")
    fun `call list_tasks no args`() {
        world.lastResult = world.toolRegistry.execute("list_tasks", emptyMap(), "/tmp")
    }

    @When("I call list_tasks with group {string}")
    fun `call list_tasks with group`(group: String) {
        world.lastResult = world.toolRegistry.execute("list_tasks", mapOf("group" to group), "/tmp")
    }

    @When("I call list_tasks with keyword {string}")
    fun `call list_tasks with keyword`(keyword: String) {
        world.lastResult = world.toolRegistry.execute("list_tasks", mapOf("keyword" to keyword), "/tmp")
    }

    @When("I call list_tasks with dryRun")
    fun `call list_tasks with dryRun`() {
        world.lastDryRun = true
        world.lastResult = world.toolRegistry.execute("list_tasks", emptyMap(), "/tmp", dryRun = true)
    }

    @Then("the result contains task {string}")
    fun `result contains task`(taskName: String) {
        assertNotNull(world.lastResult, "Result should not be null")
        assertTrue(world.lastResult.contains(taskName), "Expected to contain '$taskName' but got: ${world.lastResult}")
    }

    @Then("the result does NOT contain task {string}")
    fun `result does NOT contain task`(taskName: String) {
        assertNotNull(world.lastResult, "Result should not be null")
        assertTrue(!world.lastResult.contains(taskName), "Expected NOT to contain '$taskName' but got: ${world.lastResult}")
    }

    @Then("the result includes option {string} for task {string}")
    fun `result includes option for task`(option: String, taskName: String) {
        assertNotNull(world.lastResult, "Result should not be null")
        val lines = world.lastResult.lines()
        val headerIdx = lines.indexOfFirst { it.contains(taskName) }
        val endIdx = lines.subList(headerIdx + 1, lines.size).indexOfFirst { it.startsWith("- gradle_") || it.startsWith("---") }
        val taskBlockEnd = if (endIdx == -1) lines.size else headerIdx + 1 + endIdx
        val taskBlock = lines.subList(headerIdx, taskBlockEnd).joinToString("\n")
        assertTrue(taskBlock.contains(option), "Expected option '$option' in block:\n$taskBlock")
    }

    @Then("the result shows {string}")
    fun `result shows`(message: String) {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.contains(message), "Expected message '$message' but got: ${world.lastResult}")
    }

    @Then("the result starts with {string}")
    fun `result starts with`(prefix: String) {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.startsWith(prefix), "Expected to start with '$prefix' but got: ${world.lastResult}")
    }
}
