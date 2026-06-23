package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EpicV920Steps(private val world: IngestGovernanceWorld) {

    @Before("@epic_v_9_20")
    fun reset() {
        world.reset()
    }

    @When("I run the ingestGovernance task in chunk incremental mode")
    fun `run ingestGovernance task in chunk incremental mode`() {
        world.runTask(chunkIncremental = true)
    }

    @When("I run the ingestGovernance task in chunk incremental mode with output file {string}")
    fun `run ingestGovernance task in chunk incremental mode with output`(outputPath: String) {
        world.runTask(outputPath = outputPath, chunkIncremental = true)
    }

    @When("I run the ingestGovernance task in file and chunk incremental mode")
    fun `run ingestGovernance task in file and chunk incremental mode`() {
        world.runTask(incremental = true, chunkIncremental = true)
    }

    @Then("the chunk incremental report is not null")
    fun `chunk incremental report is not null`() {
        assertNotNull(world.lastChunkIncrementalReport, "Chunk incremental report should be populated")
    }

    @Then("the chunk incremental report has chunks added equal to {int}")
    fun `chunk incremental report chunks added equal to`(expected: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertEquals(expected, report.chunksAdded.size,
            "Expected chunksAdded.size=$expected, got ${report.chunksAdded.size}")
    }

    @Then("the chunk incremental report has chunks added greater than {int}")
    fun `chunk incremental report chunks added greater than`(min: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertTrue(report.chunksAdded.size > min,
            "Expected chunksAdded.size > $min, got ${report.chunksAdded.size}")
    }

    @Then("the chunk incremental report has chunks modified equal to {int}")
    fun `chunk incremental report chunks modified equal to`(expected: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertEquals(expected, report.chunksModified.size,
            "Expected chunksModified.size=$expected, got ${report.chunksModified.size}")
    }

    @Then("the chunk incremental report has chunks removed equal to {int}")
    fun `chunk incremental report chunks removed equal to`(expected: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertEquals(expected, report.chunksRemoved.size,
            "Expected chunksRemoved.size=$expected, got ${report.chunksRemoved.size}")
    }

    @Then("the chunk incremental report has chunks unchanged equal to {int}")
    fun `chunk incremental report chunks unchanged equal to`(expected: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertEquals(expected, report.chunksUnchanged.size,
            "Expected chunksUnchanged.size=$expected, got ${report.chunksUnchanged.size}")
    }

    @Then("the chunk incremental report has chunks unchanged greater than {int}")
    fun `chunk incremental report chunks unchanged greater than`(min: Int) {
        val report = world.lastChunkIncrementalReport ?: error("No chunk incremental report")
        assertTrue(report.chunksUnchanged.size > min,
            "Expected chunksUnchanged.size > $min, got ${report.chunksUnchanged.size}")
    }
}