package codebase.scenarios

import codebase.koog.planning.TaskResultVerifier
import codebase.koog.state.VibecodingState
import codebase.koog.VibecodingGraph
import contracts.agent.Epic
import contracts.agent.Plan
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.vibecoding.registry.ToolRegistry
import contracts.vibecoding.tools.ExecGradleTool
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.nio.file.Files
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals

class VibeHardening2Steps(private val world: VibeHardening2World) {

    @Given("a vibe hardening 2 world is initialized")
    fun `vibe hardening 2 world initialized`() {
        assertNotNull(world, "VibeHardening2World should be instantiated by PicoContainer")
    }

    @When("the vibe hardening 2 write_file tool is called with content of {int} chars")
    fun `vibe hardening 2 write_file called`(size: Int) {
        val registry = ToolRegistry()
        val tmp = Files.createTempFile("vibe-hardening-2", ".txt").toString()
        val content = "a".repeat(size)
        world.writeFileException = try {
            registry.execute("write_file", mapOf("path" to tmp, "content" to content), "/tmp")
            null
        } catch (e: IllegalArgumentException) {
            e
        }
    }

    @Then("the vibe hardening 2 write_file tool rejects the content with a size error")
    fun `vibe hardening 2 write_file rejects`() {
        val ex = world.writeFileException
        assertNotNull(ex, "write_file should reject oversized content")
        assertTrue(
            ex.message!!.contains("exceeds", ignoreCase = true),
            "Rejection message should mention 'exceeds', got: ${ex.message}"
        )
    }

    @When("the vibe hardening 2 verifier checks stdout {string} against expected {string}")
    fun `vibe hardening 2 verifier checks`(stdout: String, expected: String) {
        val result = TaskResultVerifier().verify(stdout, "", expected)
        world.verifierVerdict = result.verdict.name
    }

    @Then("the vibe hardening 2 verifier returns SUCCESS")
    fun `vibe hardening 2 verifier success`() {
        assertEquals("SUCCESS", world.verifierVerdict,
            "Expected SUCCESS verdict, got ${world.verifierVerdict}")
    }

    @When("the vibe hardening 2 gradle tool validates task {string}")
    fun `vibe hardening 2 gradle validates`(task: String) {
        world.gradleValidationException = try {
            ExecGradleTool.validateGradleTask(task)
            null
        } catch (e: SecurityException) {
            e
        }
    }

    @Then("the vibe hardening 2 gradle tool accepts the task")
    fun `vibe hardening 2 gradle accepts`() {
        assertEquals(null, world.gradleValidationException,
            "Gradle tool should accept the task, got: ${world.gradleValidationException?.message}")
    }

    @When("the vibe hardening 2 prompt is built for a {int}-task plan with {int} executed tasks")
    fun `vibe hardening 2 prompt built`(planTasks: Int, executedCount: Int) {
        val tasks = (1..planTasks).map { i ->
            PlanTask(description = "task $i", gradleTask = "tasks")
        }
        val plan = Plan(
            title = "hardening-2-remaining",
            epics = listOf(
                Epic(
                    name = "E1",
                    description = "remaining tasks test",
                    points = 1,
                    userStories = listOf(UserStory(description = "US1", tasks = tasks))
                )
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )
        val state = VibecodingState(
            intention = "remaining tasks coerce",
            workspaceRoot = "/tmp",
            maxActions = 10,
            maxRetries = 2,
            plan = plan,
            executedTasks = (1..executedCount).map { "task$it" }
        )
        val prompt = world.graph.buildPromptForIteration(state)
        world.promptRemainingLine = prompt.lineSequence()
            .firstOrNull { it.startsWith("Plan remaining tasks:") }
    }

    @Then("the vibe hardening 2 prompt remaining tasks line is {string}")
    fun `vibe hardening 2 prompt remaining line`(expected: String) {
        val line = world.promptRemainingLine
        assertNotNull(line, "Prompt should contain a 'Plan remaining tasks' line")
        assertEquals(expected, line, "Remaining tasks line mismatch")
    }
}