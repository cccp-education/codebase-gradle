package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GovernanceOntologizerSteps(private val world: GovernanceOntologizerWorld) {

    @Before("@epic_v_9_7")
    fun reset() {
        world.reset()
    }

    @Given("a chunk from source file {string}")
    fun `chunk from source file`(sourceFile: String) {
        world.lastChunk = world.buildChunk(sourceFile)
    }

    @When("I classify the chunk with GovernanceOntologizer")
    fun `classify chunk`() {
        val chunk = world.lastChunk ?: error("chunk must be set")
        world.lastSection = world.ontologizer.classify(chunk)
    }

    @Then("the governance section is {string}")
    fun `governance section is`(expected: String) {
        val section = world.lastSection ?: error("section must be set")
        assertNotNull(section)
        assertEquals(expected, section.name)
    }
}
