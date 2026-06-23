package codebase.scenarios

import codebase.koog.SessionProtocolTask
import codebase.koog.VibecodingTask
import codebase.koog.llm.FakeLlmProvider
import contracts.vibecoding.registry.ToolRegistry
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpicV914Steps {

    private lateinit var tempDir: Path
    private var toolRegistry: ToolRegistry? = null

    @Before("@epic_v_9_14")
    fun reset() {
        tempDir = Files.createTempDirectory("epic-v-9-14-")
        toolRegistry = null
    }

    @Given("a governed project with file {string} containing")
    fun `governed project file with content`(fileName: String, content: String) {
        tempDir.resolve(fileName).toFile().writeText(content.trimIndent())
    }

    @When("I run VibecodingTask dryRun with intention {string}")
    fun `run VibecodingTask dryRun`(intention: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-14-vibecoding")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("vibecode", VibecodingTask::class.java) {
            it.intention.set(intention)
            it.dryRun.set(true)
            it.maxActions.set(1)
            it.workspaceRoot.set(tempDir.toFile())
        }.get()

        task.executeVibecoding()
        toolRegistry = task.toolRegistry
    }

    @When("I run SessionProtocolTask with prompt {string}")
    fun `run SessionProtocolTask`(prompt: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-14-session-protocol")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set(prompt)
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()
        toolRegistry = task.toolRegistry
    }

    @Then("the task toolRegistry blocks {string} with command {string}")
    fun `tool registry blocks shell command`(toolName: String, command: String) {
        val r = toolRegistry ?: error("No ToolRegistry captured from task execution")
        val exception = assertFailsWith<SecurityException> {
            r.execute(toolName, mapOf("command" to command), workspaceRoot = tempDir.toString())
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [$toolName]"))
    }

    @Then("the task toolRegistry blocks {string} with task {string}")
    fun `tool registry blocks gradle task`(toolName: String, task: String) {
        val r = toolRegistry ?: error("No ToolRegistry captured from task execution")
        val exception = assertFailsWith<SecurityException> {
            r.execute(toolName, mapOf("task" to task), workspaceRoot = tempDir.toString())
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [$toolName]"))
    }
}
