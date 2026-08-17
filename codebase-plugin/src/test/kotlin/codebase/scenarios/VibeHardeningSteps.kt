package codebase.scenarios

import codebase.koog.llm.LlmProvider
import codebase.koog.state.VibecodingState
import codebase.koog.VibecodingGraph
import contracts.vibecoding.registry.ToolRegistry
import contracts.vibecoding.tools.ExecShellTool
import contracts.vibecoding.tools.ExecGradleTool
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.delay
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals

class VibeHardeningSteps(private val world: VibeHardeningWorld) {

    @Given("a vibe hardening world is initialized")
    fun `vibe hardening world initialized`() {
        assertNotNull(world, "VibeHardeningWorld should be instantiated by PicoContainer")
    }

    @When("the vibe hardening shell tool validates command {string}")
    fun `vibe hardening shell validates command`(command: String) {
        world.shellValidationException = try {
            ExecShellTool.validateCommand(command)
            null
        } catch (e: SecurityException) {
            e
        }
    }

    @Then("the vibe hardening shell tool rejects the command")
    fun `vibe hardening shell rejects`() {
        assertNotNull(world.shellValidationException, "Shell tool should reject the command")
    }

    @Then("the vibe hardening shell rejection message mentions {string}")
    fun `vibe hardening shell rejection mentions`(expected: String) {
        val ex = world.shellValidationException
        assertNotNull(ex, "Shell rejection should have happened")
        val msg = ex.message ?: ""
        assertTrue(
            msg.contains(expected, ignoreCase = true),
            "Rejection message should mention '$expected', got: $msg"
        )
    }

    @When("the vibe hardening gradle tool validates task {string}")
    fun `vibe hardening gradle validates task`(task: String) {
        world.gradleValidationException = try {
            ExecGradleTool.validateGradleTask(task)
            null
        } catch (e: SecurityException) {
            e
        }
    }

    @Then("the vibe hardening gradle tool accepts the task")
    fun `vibe hardening gradle accepts`() {
        assertEquals(null, world.gradleValidationException,
            "Gradle tool should accept the task, got: ${world.gradleValidationException?.message}")
    }

    @Given("a slow LLM provider that sleeps for {int} ms")
    fun `vibe hardening slow llm provider`(sleepMs: Int) {
        world.slowLlmProvider = LlmProvider { delay(sleepMs.toLong()); "late" }
    }

    @Given("a vibe hardening graph configured with llmTimeoutMs {int}")
    fun `vibe hardening graph with timeout`(timeoutMs: Int) {
        val provider = world.slowLlmProvider
            ?: throw IllegalStateException("slowLlmProvider must be configured first")
        world.graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = provider,
            llmTimeoutMs = timeoutMs.toLong()
        )
    }

    @When("I execute vibe hardening with intention {string} and maxActions {int}")
    fun `vibe hardening execute`(intention: String, maxActions: Int) {
        val state = VibecodingState(
            intention = intention,
            workspaceRoot = "/tmp",
            maxActions = maxActions
        )
        world.resultState = world.graph.execute(state)
        assertNotNull(world.resultState, "Result state should not be null")
    }

    @Then("the vibe hardening result state is not null")
    fun `vibe hardening result not null`() {
        assertNotNull(world.resultState, "Result state should not be null")
    }

    @Then("the vibe hardening result has an error")
    fun `vibe hardening result has error`() {
        val state = world.resultState
        assertNotNull(state, "Result state must not be null")
        assertTrue(state.error != null, "Expected error, got null")
    }

    @Then("the vibe hardening error contains {string}")
    fun `vibe hardening error contains`(expected: String) {
        val state = world.resultState
        assertNotNull(state, "Result state must not be null")
        val error = state.error ?: throw AssertionError("No error in result state")
        assertTrue(
            error.contains(expected, ignoreCase = true),
            "Error should contain '$expected', got: $error"
        )
    }

    @Given("a vibe hardening graph initialized with fake LLM for error recovery")
    fun `vibe hardening graph with fake llm`() {
        world.initGraphWithFakeLLM()
    }

    @Given("the vibe hardening fake LLM suggests the next response {string}")
    fun `vibe hardening fake llm suggests`(response: String) {
        world.fakeLlmProvider?.let { it.nextResponse = response }
            ?: throw IllegalStateException("Fake LLM not initialized")
    }

    @When("I execute vibe hardening with a {int}-task failing plan and maxRetries {int}")
    fun `vibe hardening execute failing plan`(taskCount: Int, maxRetries: Int) {
        val tasks = (1..taskCount).map { i ->
            contracts.agent.GradleTask(
                description = "Task $i: will fail",
                gradleTask = "nonexistentTask${i}"
            )
        }
        val plan = contracts.agent.Plan(
            title = "hardening-failing-plan",
            epics = listOf(
                contracts.agent.Epic(
                    name = "FAIL",
                    description = "Failing epic",
                    points = 1,
                    userStories = listOf(
                        contracts.agent.UserStory(description = "US-fail", tasks = tasks)
                    )
                )
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )
        val state = VibecodingState(
            intention = "Hardening retry counter test",
            workspaceRoot = "/tmp",
            maxActions = 10,
            maxRetries = maxRetries,
            plan = plan,
            planJson = "{}"
        )
        world.resultState = world.graph.execute(state)
    }

    @Then("the vibe hardening retry count is at most {int}")
    fun `vibe hardening retry count at most`(max: Int) {
        val state = world.resultState
        assertNotNull(state, "Result state must not be null")
        assertTrue(
            state.retryCount <= max,
            "retryCount should be at most $max (single increment per retry), got ${state.retryCount}"
        )
    }
}