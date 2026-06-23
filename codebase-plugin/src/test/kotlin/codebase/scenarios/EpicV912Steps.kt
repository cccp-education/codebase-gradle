package codebase.scenarios

import codebase.koog.agentic.AgenticExecutor
import codebase.koog.agentic.IngestGovernanceTask
import codebase.koog.agentic.IngestionReport
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EpicV912Steps {

    private lateinit var tempDir: Path
    private var report: IngestionReport? = null
    private var executor: AgenticExecutor? = null

    @Before("@epic_v_9_12")
    fun reset() {
        tempDir = Files.createTempDirectory("epic-v-9-12-")
        report = null
        executor = null
    }

    @Given("a governance file {string} with content")
    fun `governance file with content`(fileName: String, content: String) {
        tempDir.resolve(fileName).toFile().writeText(content.trimIndent())
    }

    @When("I ingest the governance file")
    fun `ingest the governance file`() {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        report = task.lastIngestionReport
        executor = task.lastExecutor
    }

    @Then("the ingestion report has executables")
    fun `ingestion report has executables`() {
        val r = report ?: error("No ingestion report")
        assertTrue(r.artifactsCompiled > 0, "Expected compiled artifacts, got ${r.artifactsCompiled}")
        assertTrue(r.executables.isNotEmpty(), "Expected executables in report")
    }

    @Then("the executor blocks tool {string} with command {string}")
    fun `executor blocks shell command`(toolName: String, command: String) {
        val ex = executor ?: error("No executor")
        val result = ex.check(toolName, mapOf("command" to command))
        assertFalse(result.allowed, "Expected $toolName '$command' to be blocked")
        assertNotNull(result.ruleId)
    }

    @Then("the executor blocks tool {string} with task {string}")
    fun `executor blocks gradle task`(toolName: String, task: String) {
        val ex = executor ?: error("No executor")
        val result = ex.check(toolName, mapOf("task" to task))
        assertFalse(result.allowed, "Expected $toolName '$task' to be blocked")
        assertNotNull(result.ruleId)
    }

    @Then("the executor allows tool {string} with command {string}")
    fun `executor allows shell command`(toolName: String, command: String) {
        val ex = executor ?: error("No executor")
        val result = ex.check(toolName, mapOf("command" to command))
        assertTrue(result.allowed, "Expected $toolName '$command' to be allowed")
    }

    @Then("the executor allows tool {string} with task {string}")
    fun `executor allows gradle task`(toolName: String, task: String) {
        val ex = executor ?: error("No executor")
        val result = ex.check(toolName, mapOf("task" to task))
        assertTrue(result.allowed, "Expected $toolName '$task' to be allowed")
    }
}
