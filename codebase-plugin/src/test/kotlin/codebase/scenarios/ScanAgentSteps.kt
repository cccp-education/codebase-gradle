package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanAgentSteps(private val world: ScanAgentWorld) {

    @Before("@epic_v_9_3")
    fun reset() {
        world.reset()
    }

    @Given("an empty workspace directory")
    fun `empty workspace directory`() {
        // workspaceDir is reset to empty in Before hook
    }

    @Given("a workspace directory with the following adoc files at root")
    fun `workspace with adoc files at root`(table: io.cucumber.datatable.DataTable) {
        table.asMaps().forEach { row ->
            val filename = row["filename"] ?: error("filename column required")
            val content = (row["content"] ?: "").replace("\\n", "\n")
            world.file(filename).writeText(content)
        }
    }

    @Given("a workspace directory with a nested .agents structure")
    fun `workspace with nested agents structure`() {
        world.file("AGENT.adoc").writeText("= Agent\n")
        val agentsDir = world.file(".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Index\n")
        File(agentsDir, "SESSIONS_HISTORY.adoc").writeText("= History\n")
        val sessionsDir = File(agentsDir, "sessions").apply { mkdirs() }
        File(sessionsDir, "001-test.adoc").writeText("= Session 1\n")
    }

    @Given("a workspace directory with a subproject containing .agents files")
    fun `workspace with subproject agents files`() {
        val sub = world.file("my-plugin").apply { mkdirs() }
        File(sub, "AGENT.adoc").writeText("= Agent\n")
        val agentsDir = File(sub, ".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Index\n")
    }

    @Given("a workspace directory with adoc files in build and .git directories")
    fun `workspace with adoc in build and git`() {
        world.file("AGENT.adoc").writeText("= Agent\n")
        val buildDir = world.file("build").apply { mkdirs() }
        File(buildDir, "generated.adoc").writeText("= Generated\n")
        val gitDir = world.file(".git").apply { mkdirs() }
        File(gitDir, "README.adoc").writeText("= Git\n")
    }

    @Given("a workspace directory with adoc files at root and in .agents")
    fun `workspace with adoc at root and agents`() {
        world.file("AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS committer\n")
        val agentsDir = world.file(".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Index\n\n* [ ] Item\n")
    }

    @When("I scan the directory with ScanAgent")
    fun `scan directory`() {
        world.scan()
    }

    @When("IngestGovernanceTask ingests the workspace")
    fun `ingest workspace`() {
        world.ingestWorkspace()
    }

    @Then("the scanned files list is empty")
    fun `scanned files empty`() {
        assertTrue(world.scannedFiles.isEmpty(), "Expected empty file list, got ${world.scannedFiles.size}")
    }

    @Then("the scanned files count is {int}")
    fun `scanned files count`(expected: Int) {
        assertEquals(expected, world.scannedFiles.size, "Scanned files count mismatch")
    }

    @Then("the scanned files contain {string}")
    fun `scanned files contain`(path: String) {
        assertTrue(
            world.scannedFiles.any { it.relativePath == path || it.relativePath.contains(path) },
            "Expected file path containing '$path' not found in: ${world.scannedFiles.map { it.relativePath }}"
        )
    }

    @Then("the scanned files do not contain {string}")
    fun `scanned files do not contain`(path: String) {
        assertFalse(
            world.scannedFiles.any { it.relativePath.contains(path) },
            "Unexpected file path containing '$path' found in: ${world.scannedFiles.map { it.relativePath }}"
        )
    }

    @Then("the ingestion report files scanned is greater than {int}")
    fun `ingestion files scanned greater than`(min: Int) {
        val report = world.ingestionReport ?: error("No ingestion report")
        assertTrue(report.filesScanned > min, "filesScanned=${report.filesScanned} should be > $min")
    }

    @Then("the ingestion report chunks added is greater than {int}")
    fun `ingestion chunks added greater than`(min: Int) {
        val report = world.ingestionReport ?: error("No ingestion report")
        assertTrue(report.chunksAdded > min, "chunksAdded=${report.chunksAdded} should be > $min")
    }
}