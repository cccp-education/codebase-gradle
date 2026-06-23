package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpicV919Steps(private val world: IngestGovernanceWorld) {

    @Before("@epic_v_9_19")
    fun reset() {
        world.reset()
    }

    @When("I run the ingestGovernance task in incremental mode")
    fun `run ingestGovernance task in incremental mode`() {
        world.runTask(incremental = true)
    }

    @When("I run the ingestGovernance task in incremental mode with output file {string}")
    fun `run ingestGovernance task in incremental mode with output`(outputPath: String) {
        world.runTask(outputPath = outputPath, incremental = true)
    }

    @Then("the incremental report lists {string} as added")
    fun `incremental report lists as added`(path: String) {
        val report = world.lastIncrementalReport ?: error("No incremental report")
        assertTrue(report.added.contains(path),
            "Expected $path in added, got ${report.added}")
    }

    @Then("the incremental report lists {string} as modified")
    fun `incremental report lists as modified`(path: String) {
        val report = world.lastIncrementalReport ?: error("No incremental report")
        assertTrue(report.modified.contains(path),
            "Expected $path in modified, got ${report.modified}")
    }

    @Then("the incremental report lists {string} as unchanged")
    fun `incremental report lists as unchanged`(path: String) {
        val report = world.lastIncrementalReport ?: error("No incremental report")
        assertTrue(report.unchanged.contains(path),
            "Expected $path in unchanged, got ${report.unchanged}")
    }

    @Then("the incremental report lists {string} as removed")
    fun `incremental report lists as removed`(path: String) {
        val report = world.lastIncrementalReport ?: error("No incremental report")
        assertTrue(report.removed.contains(path),
            "Expected $path in removed, got ${report.removed}")
    }

    @Then("the incremental report is not null")
    fun `incremental report is not null`() {
        assertNotNull(world.lastIncrementalReport, "Incremental report should be populated")
    }

    @Then("the ingestion report has files scanned equal to {int}")
    fun `ingestion report files scanned equal to`(expected: Int) {
        val report = world.lastReport ?: error("No ingestion report")
        assertEquals(expected, report.filesScanned,
            "Expected filesScanned=$expected, got ${report.filesScanned}")
    }

    @Then("the ingestion report has files scanned greater than {int}")
    fun `ingestion report files scanned greater than`(min: Int) {
        val report = world.lastReport ?: error("No ingestion report")
        assertTrue(report.filesScanned > min,
            "Expected filesScanned > $min, got ${report.filesScanned}")
    }
}