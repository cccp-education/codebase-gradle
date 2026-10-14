package codebase.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@koog-pipeline` scenarios (EPIC KOOG-PIPELINE-DEDUP,
 * BS-1).
 *
 * Steps are prefixed "koog pipeline" to avoid glue collisions with other
 * feature step classes sharing the `codebase.scenarios` package (pattern
 * S-088). Pure BDD: the real `codebase.koog.pipeline` skeleton is driven via
 * the in-memory [KoogPipelineDefinition] — no Gradle task, no network.
 */
class KoogPipelineSteps(private val world: KoogPipelineWorld) {

    @Given("a koog pipeline world is initialized")
    fun `koog pipeline world initialized`() {
        assertNotNull(world, "KoogPipelineWorld should be instantiated by PicoContainer")
    }

    @Given("a koog pipeline that proposes a valid plan")
    fun `koog pipeline proposes a valid plan`() {
        world.proposeThrows = null
    }

    @Given("a koog pipeline that throws on propose with {string}")
    fun `koog pipeline throws on propose`(message: String) {
        world.proposeThrows = message
    }

    @Given("a koog pipeline that rejects validation with {string}")
    fun `koog pipeline rejects validation`(message: String) {
        world.validateError = message
    }

    @When("the koog pipeline executes")
    fun `koog pipeline executes`() {
        world.execute()
    }

    @When("the koog pipeline mermaid diagram is requested")
    fun `koog pipeline mermaid diagram requested`() {
        world.requestMermaidDiagram()
    }

    @Then("the koog pipeline call order is {string}")
    fun `koog pipeline call order`(expected: String) {
        val definition = world.definition
        assertNotNull(definition, "Koog pipeline definition not built")
        assertEquals(expected, definition.calls.joinToString(","))
    }

    @Then("the koog pipeline final stage is {string}")
    fun `koog pipeline final stage`(expected: String) {
        val result = world.result
        assertNotNull(result, "Koog pipeline not executed")
        assertEquals(expected, result.stage.name)
    }

    @Then("the koog pipeline output is {string}")
    fun `koog pipeline output`(expected: String) {
        val result = world.result
        assertNotNull(result, "Koog pipeline not executed")
        assertEquals(expected, result.output)
    }

    @Then("the koog pipeline validation error is {string}")
    fun `koog pipeline validation error`(expected: String) {
        val result = world.result
        assertNotNull(result, "Koog pipeline not executed")
        assertEquals(expected, result.validationError)
    }

    @Then("the koog pipeline error is {string}")
    fun `koog pipeline error`(expected: String) {
        val result = world.result
        assertNotNull(result, "Koog pipeline not executed")
        assertEquals(expected, result.error)
    }

    @Then("the koog pipeline mermaid diagram is not blank")
    fun `koog pipeline mermaid diagram not blank`() {
        val mermaid = world.mermaidDiagram
        assertNotNull(mermaid, "Koog pipeline mermaid diagram not requested")
        assertTrue(mermaid.isNotBlank(), "Expected a non-blank mermaid diagram")
    }
}
