package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.planning.RollbackStrategyExecutor
import contracts.agent.Epic
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.vibecoding.registry.ToolRegistry
import codebase.koog.planning.Plan
import codebase.koog.state.VibecodingState
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class E2EPlanFailAdaptSteps(private val world: E2EPlanFailAdaptWorld) {

    @Before("@epic_x_6")
    fun reset() {
        world.reset()
    }

    @Given("a VibecodingGraph with FakeLlmProvider and RollbackStrategyExecutor")
    fun `graph with fake llm and rollback`() {
        world.fakeLlm = FakeLlmProvider()
        world.toolRegistry = ToolRegistry()
        world.toolRegistry!!.registerHandler("exec_gradle") { _, args, _ ->
            val task = args["task"] ?: "unknown"
            val idx = world.gradleCallCount
            world.gradleCallCount++
            if (idx < world.gradleResponses.size) {
                world.gradleResponses[idx]
            } else {
                "BUILD SUCCESSFUL in 1s"
            }
        }
        val rollbackExecutor = RollbackStrategyExecutor(world.toolRegistry!!, "/tmp/test")
        world.graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = world.toolRegistry!!,
            llmProvider = world.fakeLlm,
            rollbackExecutor = rollbackExecutor
        )
    }

    @Given("a VibecodingState with plan {string} and maxRetries {int}")
    fun `state with plan and maxRetries`(planDesc: String, maxRetries: Int) {
        val steps = planDesc.split("→").map { it.trim() }
        val plan = Plan(
            title = "E2E test plan",
            epics = listOf(
                Epic(name = "E1", description = "test", points = steps.size, userStories = listOf(
                    UserStory(description = "US1", tasks = steps.map { step ->
                        PlanTask(description = step, gradleTask = step)
                    })
                ))
            ),
            totalPoints = steps.size,
            estimatedSessions = "1"
        )
        world.state = VibecodingState(
            intention = "E2E test: $planDesc",
            workspaceRoot = "/tmp/test",
            maxActions = 10,
            maxRetries = maxRetries,
            plan = plan
        )
    }

    @Given("a VibecodingState with plan {string} and maxRetries {int} and rollbackStrategy {word}")
    fun `state with plan maxRetries and rollbackStrategy`(planDesc: String, maxRetries: Int, strategy: String) {
        val steps = planDesc.split("→").map { it.trim() }
        val plan = Plan(
            title = "E2E test plan",
            epics = listOf(
                Epic(name = "E1", description = "test", points = steps.size, userStories = listOf(
                    UserStory(description = "US1", tasks = steps.map { step ->
                        PlanTask(description = step, gradleTask = step)
                    })
                ))
            ),
            totalPoints = steps.size,
            estimatedSessions = "1"
        )
        world.state = VibecodingState(
            intention = "E2E test: $planDesc",
            workspaceRoot = "/tmp/test",
            maxActions = 10,
            maxRetries = maxRetries,
            rollbackStrategy = strategy,
            plan = plan
        )
    }

    @Given("the first compile will fail with {string}")
    fun `first compile fails`(errorMsg: String) {
        world.gradleResponses.add("BUILD FAILED in 2s\n$errorMsg")
    }

    @Given("the LLM will suggest {string}")
    fun `llm suggests`(suggestion: String) {
        world.fakeLlm!!.enqueueResponse(suggestion)
    }

    @Given("the next task will succeed")
    fun `next task succeeds`() {
        world.gradleResponses.add("BUILD SUCCESSFUL in 3s")
    }

    @Given("the compile will always fail with {string}")
    fun `compile always fails`(errorMsg: String) {
        world.gradleResponses.add("BUILD FAILED in 2s\n$errorMsg")
        world.gradleResponses.add("BUILD FAILED in 2s\n$errorMsg")
        world.gradleResponses.add("BUILD FAILED in 2s\n$errorMsg")
    }

    @When("the agent executes the full pipeline")
    fun `execute full pipeline`() {
        assertNotNull(world.graph, "Graph must be set")
        assertNotNull(world.state, "State must be set")
        world.result = world.graph!!.execute(world.state!!)
    }

    @Then("all {int} steps are executed")
    fun `all N steps executed`(count: Int) {
        assertNotNull(world.result)
        assertEquals(count, world.result!!.executedTasks.size)
    }

    @Then("the final state is finished")
    fun `final state is finished`() {
        assertNotNull(world.result)
        assertTrue(world.result!!.finished)
    }

    @Then("the final state is not finished")
    fun `final state is not finished`() {
        assertNotNull(world.result)
        assertTrue(!world.result!!.finished)
    }

    @Then("the final error is null")
    fun `final error is null`() {
        assertNotNull(world.result)
        assertNull(world.result!!.error)
    }

    @Then("the final error contains {string}")
    fun `final error contains`(text: String) {
        assertNotNull(world.result)
        assertNotNull(world.result!!.error)
        assertTrue(world.result!!.error!!.contains(text))
    }

    @Then("the final lastToolResult contains {string}")
    fun `final lastToolResult contains`(text: String) {
        assertNotNull(world.result)
        assertTrue(world.result!!.lastToolResult.contains(text))
    }
}
