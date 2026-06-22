package codebase.scenarios

import codebase.koog.agentic.GovernanceSection
import io.cucumber.datatable.DataTable
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IngestGovernanceSteps(private val world: IngestGovernanceWorld) {

    @Before("@epic_v_9_8")
    fun reset() {
        world.reset()
    }

    @Given("a temporary project with governance files")
    fun `temporary project with governance files`(table: DataTable) {
        table.asMaps().forEach { row ->
            val sourceFile = row["sourceFile"] ?: error("sourceFile column required")
            val content = (row["content"] ?: "").replace("\\n", "\n")
            world.writeFile(sourceFile, content)
        }
    }

    @Given("a temporary project with governance file {string}")
    fun `temporary project with governance file`(sourceFile: String, content: String) {
        world.writeFile(sourceFile, content.trimIndent())
    }

    @When("I run the ingestGovernance task on the project")
    fun `run ingestGovernance task`() {
        world.runTask()
    }

    @When("I run the ingestGovernance task with output file {string}")
    fun `run ingestGovernance task with output`(outputPath: String) {
        world.runTask(outputPath)
    }

    @Then("the ingestion report has chunks invalid equal to {int}")
    fun `ingestion report has chunks invalid equal to`(expected: Int) {
        val report = world.lastReport ?: error("No ingestion report")
        assertEquals(expected, report.chunksInvalid,
            "Expected chunksInvalid=$expected, got ${report.chunksInvalid}")
    }

    @Then("the ingestion report has validation errors count equal to {int}")
    fun `ingestion report has validation errors count equal to`(expected: Int) {
        val report = world.lastReport ?: error("No ingestion report")
        assertEquals(expected, report.validationErrors.size,
            "Expected validationErrors count=$expected, got ${report.validationErrors.size}")
    }

    @Then("the ingestion report has sections added")
    fun `ingestion report has sections added`(table: DataTable) {
        table.asMaps().forEach { row ->
            val section = row["section"] ?: error("section column required")
            world.assertSectionAdded(GovernanceSection.valueOf(section))
        }
    }

    @Then("the ingestion report has sections total")
    fun `ingestion report has sections total`(table: DataTable) {
        table.asMaps().forEach { row ->
            val section = row["section"] ?: error("section column required")
            world.assertSectionTotal(GovernanceSection.valueOf(section))
        }
    }

    @Then("the output JSON contains {string}")
    fun `output json contains`(keyword: String) {
        val json = world.outputJson()
        assertTrue(json.contains(keyword), "JSON should contain '$keyword', got: ${json.take(500)}")
    }

    @Then("the ingestion report section total is {string} with count greater than {int}")
    fun `section total greater than`(sectionName: String, min: Int) {
        world.assertSectionTotalGreaterThan(sectionName, min)
    }
}
