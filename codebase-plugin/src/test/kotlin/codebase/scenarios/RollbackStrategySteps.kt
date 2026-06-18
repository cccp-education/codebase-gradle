package codebase.scenarios

import codebase.koog.planning.RollbackStrategy
import codebase.koog.planning.RollbackStrategyExecutor
import codebase.koog.planning.VibecodingPlan
import codebase.koog.planning.VibecodingStep
import codebase.koog.state.VibecodingState
import contracts.vibecoding.registry.ToolRegistry
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RollbackStrategySteps(private val world: RollbackStrategyWorld) {

    @Before("@epic_x_4")
    fun reset() {
        world.reset()
    }

    @Given("a RollbackStrategyExecutor with workspace {string}")
    fun `executor with workspace`(workspace: String) {
        world.workspaceRoot = workspace
        val registry = ToolRegistry()
        registry.registerHandler("exec_shell") { _, args, _ ->
            "EXIT 0\nChecked out ${args["command"]}"
        }
        world.executor = RollbackStrategyExecutor(registry, world.workspaceRoot)
    }

    @Given("a VibecodingState with retryCount {int} and maxRetries {int}")
    fun `state with retries`(retryCount: Int, maxRetries: Int) {
        world.state = VibecodingState(
            intention = "test",
            workspaceRoot = world.workspaceRoot,
            retryCount = retryCount,
            maxRetries = maxRetries
        )
    }

    @Given("a VibecodingPlan with strategy {word} and step {string} task {string}")
    fun `plan with strategy and step`(strategyName: String, description: String, gradleTask: String) {
        val strategy = RollbackStrategy.valueOf(strategyName)
        val step = VibecodingStep(description, gradleTask, "BUILD SUCCESSFUL")
        world.plan = VibecodingPlan(listOf(step), rollbackStrategy = strategy)
    }

    @Given("modified files {string}")
    fun `modified files`(files: String) {
        world.modifiedFiles = files.split(",").map { it.trim() }
    }

    @When("the executor executes the rollback")
    fun `execute rollback`() {
        assertNotNull(world.state, "State must be set")
        assertNotNull(world.plan, "Plan must be set")
        assertNotNull(world.executor, "Executor must be set")
        val failedStep = world.plan!!.steps[0]
        world.result = world.executor!!.execute(world.state!!, world.plan!!, failedStep, world.modifiedFiles)
    }

    @Then("the state is finished")
    fun `state is finished`() {
        assertNotNull(world.result)
        assertTrue(world.result!!.finished)
    }

    @Then("the state is not finished")
    fun `state is not finished`() {
        assertNotNull(world.result)
        assertTrue(!world.result!!.finished)
    }

    @Then("the error contains {string}")
    fun `error contains`(text: String) {
        assertNotNull(world.result)
        assertNotNull(world.result!!.error)
        assertTrue(world.result!!.error!!.contains(text))
    }

    @Then("the error is cleared")
    fun `error is cleared`() {
        assertNotNull(world.result)
        assertNull(world.result!!.error)
    }

    @Then("the retryCount is reset to {int}")
    fun `retryCount reset`(expected: Int) {
        assertNotNull(world.result)
        assertEquals(expected, world.result!!.retryCount)
    }

    @Then("the lastToolResult contains {string}")
    fun `lastToolResult contains`(text: String) {
        assertNotNull(world.result)
        assertTrue(world.result!!.lastToolResult.contains(text))
    }
}
