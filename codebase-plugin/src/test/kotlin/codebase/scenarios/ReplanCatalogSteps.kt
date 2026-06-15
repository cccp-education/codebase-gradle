package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.discovery.TaskOption
import codebase.koog.discovery.TaskSchema
import codebase.koog.llm.LlmProvider
import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.StepVerifier
import contracts.vibecoding.registry.ToolRegistry
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReplanCatalogSteps(private val world: ReplanCatalogWorld) {

    @Before("@epic_x_5")
    fun reset() {
        world.reset()
    }

    @Given("a VibecodingGraph with task schemas")
    fun `graph with task schemas`() {
        world.taskSchemas = listOf(
            TaskSchema("build", "Compiles the project", "build", "DefaultTask", emptyList()),
            TaskSchema("test", "Runs unit tests", "verification", "Test", emptyList()),
            TaskSchema("compileKotlin", "Compiles Kotlin sources", "build", "DefaultTask", emptyList())
        )
        world.graph = VibecodingGraph(
            toolRegistry = ToolRegistry(),
            taskSchemas = world.taskSchemas
        )
    }

    @Given("a VibecodingGraph without task schemas")
    fun `graph without task schemas`() {
        world.taskSchemas = emptyList()
        world.graph = VibecodingGraph(
            toolRegistry = ToolRegistry(),
            taskSchemas = emptyList()
        )
    }

    @Given("a VibecodingGraph with {int} task schemas")
    fun `graph with N task schemas`(count: Int) {
        world.taskSchemas = (1..count).map { i ->
            TaskSchema("task$i", "Task number $i", "custom", "DefaultTask", emptyList())
        }
        world.graph = VibecodingGraph(
            toolRegistry = ToolRegistry(),
            taskSchemas = world.taskSchemas
        )
    }

    @Given("a VibecodingState with error {string} and retryCount {int} and maxRetries {int}")
    fun `state with error and retries`(error: String, retryCount: Int, maxRetries: Int) {
        world.state = vibecoding.contracts.state.VibecodingState(
            intention = "test",
            workspaceRoot = "/tmp/test",
            error = error,
            retryCount = retryCount,
            maxRetries = maxRetries,
            currentTaskDescription = "compile",
            lastToolResult = "BUILD FAILED"
        )
    }

    @When("the replan prompt is built")
    fun `build replan prompt`() {
        assertNotNull(world.graph, "Graph must be set")
        assertNotNull(world.state, "State must be set")
        world.capturedPrompt = world.graph!!.buildReplanPrompt(world.state!!)
    }

    @Then("the prompt contains {string}")
    fun `prompt contains`(text: String) {
        assertNotNull(world.capturedPrompt)
        assertTrue(world.capturedPrompt!!.contains(text))
    }

    @Then("the prompt does not contain {string}")
    fun `prompt does not contain`(text: String) {
        assertNotNull(world.capturedPrompt)
        assertTrue(!world.capturedPrompt!!.contains(text))
    }

    @Then("the prompt contains at least one task name")
    fun `prompt contains at least one task name`() {
        assertNotNull(world.capturedPrompt)
        val hasTask = world.taskSchemas.any { schema ->
            world.capturedPrompt!!.contains(schema.name)
        }
        assertTrue(hasTask)
    }

    @Then("the prompt contains all {int} task names")
    fun `prompt contains all N task names`(count: Int) {
        assertNotNull(world.capturedPrompt)
        world.taskSchemas.forEach { schema ->
            assertTrue(world.capturedPrompt!!.contains(schema.name))
        }
    }
}
