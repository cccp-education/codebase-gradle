package codebase.scenarios

import codebase.koog.agentic.IngestGovernanceTask
import codebase.koog.agentic.IngestionReport
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpicV913Steps {

    private lateinit var tempDir: Path
    private var report: IngestionReport? = null
    private var registry: ToolRegistry? = null

    @Before("@epic_v_9_13")
    fun reset() {
        tempDir = Files.createTempDirectory("epic-v-9-13-")
        report = null
        registry = null
    }

    @Given("a governed file {string} containing")
    fun `governed file with content`(fileName: String, content: String) {
        tempDir.resolve(fileName).toFile().writeText(content.trimIndent())
    }

    @Given("I run ingestGovernance on it")
    fun `ingest the governance file`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        report = task.lastIngestionReport
        registry = ToolRegistry(enforcementHook = task.buildEnforcementHook())
    }

    @When("I wire the ingestion hook into a ToolRegistry")
    fun `wire the ingestion hook into a ToolRegistry`() {
        val r = report ?: error("No ingestion report; call 'I ingest the governance file' first")
        assertTrue(r.executables.isNotEmpty(), "Expected executables to produce enforcement hook")
        assertNotNull(registry, "ToolRegistry should already be wired after ingestion")
    }

    @Then("ToolRegistry blocks {string} with command {string}")
    fun `tool registry blocks shell command`(toolName: String, command: String) {
        val r = registry ?: error("No ToolRegistry")
        val exception = assertFailsWith<SecurityException> {
            r.execute(toolName, mapOf("command" to command), workspaceRoot = tempDir.toString())
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [$toolName]"))
    }

    @Then("ToolRegistry blocks {string} with task {string}")
    fun `tool registry blocks gradle task`(toolName: String, task: String) {
        val r = registry ?: error("No ToolRegistry")
        val exception = assertFailsWith<SecurityException> {
            r.execute(toolName, mapOf("task" to task), workspaceRoot = tempDir.toString())
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [$toolName]"))
    }

    @Then("ToolRegistry allows {string} with command {string}")
    fun `tool registry allows shell command`(toolName: String, command: String) {
        val r = registry ?: error("No ToolRegistry")
        r.registerHandler(toolName) { _, _, _ -> "ok" }
        val result = r.execute(toolName, mapOf("command" to command), workspaceRoot = tempDir.toString())
        assertTrue(result.contains("ok"))
    }

    @Then("ToolRegistry allows {string} with task {string}")
    fun `tool registry allows gradle task`(toolName: String, task: String) {
        val r = registry ?: error("No ToolRegistry")
        r.registerHandler(toolName) { _, _, _ -> "ok" }
        val result = r.execute(toolName, mapOf("task" to task), workspaceRoot = tempDir.toString())
        assertTrue(result.contains("ok"))
    }
}
