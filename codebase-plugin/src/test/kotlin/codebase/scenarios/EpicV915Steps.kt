package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.ChunkValidationError
import codebase.koog.agentic.ChunkValidationErrorType
import codebase.koog.agentic.ChunkValidator
import codebase.koog.agentic.IngestGovernanceTask
import codebase.koog.agentic.ValidationResult
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EpicV915Steps {

    private lateinit var tempDir: Path
    private var lastReport: codebase.koog.agentic.IngestionReport? = null
    private var lastOutputFile: File? = null
    private var customValidator: ChunkValidator? = null
    private var strictValidationEnabled: Boolean = false
    private var lastException: Throwable? = null

    @Before("@epic_v_9_15 or @epic_v_9_16")
    fun reset() {
        tempDir = Files.createTempDirectory("epic-v-9-")
        lastReport = null
        lastOutputFile = null
        customValidator = null
        strictValidationEnabled = false
        lastException = null
    }

    @Given("a quarantine test project with file {string} containing")
    fun `quarantine project file with content`(fileName: String, content: String) {
        tempDir.resolve(fileName).toFile().writeText(content.trimIndent())
    }

    @Given("a chunk validator that rejects every chunk with checksum mismatch")
    fun `validator rejects all chunks`() {
        customValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val error = ChunkValidationError(
                    sourceFile = chunk.sourceFile,
                    sourceLines = chunk.sourceLines,
                    lineStart = 1,
                    lineEnd = 1,
                    errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                    message = "checksum does not match content (injected by Cucumber)"
                )
                return ValidationResult(valid = false, errors = listOf(error))
            }
        }
    }

    @When("I run IngestGovernanceTask for quarantine on the project")
    fun `run ingest governance task for quarantine`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-15-ingest")
            .build()

        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            customValidator?.let { validator -> it.chunkValidator = validator }
        }.get()

        task.executeIngest()
        lastReport = task.lastIngestionReport
    }

    @When("I run IngestGovernanceTask for quarantine with output file {string}")
    fun `run ingest governance task for quarantine with output`(outputFileName: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-15-json")
            .build()

        val outputFile = tempDir.resolve(outputFileName).toFile()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
            customValidator?.let { validator -> it.chunkValidator = validator }
        }.get()

        task.executeIngest()
        lastOutputFile = outputFile
    }

    @When("I run IngestGovernanceTask with strict validation enabled")
    fun `run ingest governance task with strict validation`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-16-strict")
            .build()

        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.strictValidation.set(true)
            customValidator?.let { validator -> it.chunkValidator = validator }
        }.get()

        try {
            task.executeIngest()
            lastReport = task.lastIngestionReport
        } catch (e: Throwable) {
            lastException = e
        }
    }

    @Then("the ingestion report should contain quarantined chunks")
    fun `report contains quarantined chunks`() {
        val report = lastReport ?: error("No IngestionReport captured")
        assertTrue(report.invalidChunks.isNotEmpty(), "Expected quarantined chunks in report")
    }

    @Then("the ingestion report should contain no quarantined chunks")
    fun `report contains no quarantined chunks`() {
        val report = lastReport ?: error("No IngestionReport captured")
        assertTrue(report.invalidChunks.isEmpty(), "Expected no quarantined chunks in report")
    }

    @Then("each quarantined chunk should have at least one error")
    fun `quarantined chunks have errors`() {
        val report = lastReport ?: error("No IngestionReport captured")
        assertTrue(report.invalidChunks.all { it.errors.isNotEmpty() },
            "Every quarantined chunk should carry validation errors")
    }

    @Then("the task should succeed")
    fun `task should succeed`() {
        assertNull(lastException, "Task should not have thrown an exception")
    }

    @Then("the task should fail with a strict validation error")
    fun `task should fail with strict validation error`() {
        assertNotNull(lastException, "Task should have thrown an exception")
        assertTrue(lastException!!.message!!.contains("strict validation"),
            "Exception should mention strict validation, but was: ${lastException!!.message}")
    }

    @Then("the error should mention the quarantined chunk error type")
    fun `error should mention quarantined chunk error type`() {
        assertNotNull(lastException, "Task should have thrown an exception")
        assertTrue(lastException!!.message!!.contains("CHECKSUM_MISMATCH"),
            "Exception should mention CHECKSUM_MISMATCH, but was: ${lastException!!.message}")
    }

    @Then("the output file should contain {string}")
    fun `output file contains token`(token: String) {
        val file = lastOutputFile ?: error("No output file captured")
        assertTrue(file.exists(), "Output file should exist")
        val content = file.readText()
        assertTrue(content.contains(token), "Expected JSON to contain '$token' but was:\n$content")
    }
}
