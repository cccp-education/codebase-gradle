package codebase.scenarios

import codebase.koog.planning.TaskVerdict
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskResultVerifierSteps(private val world: TaskResultVerifierWorld) {

    @Before("@epic_x_2")
    fun reset() {
        world.reset()
    }

    @Given("a TaskResultVerifier")
    fun `given verifier`() {
        world.reset()
    }

    @When("I verify stdout {string} and stderr {string}")
    fun `verify stdout and stderr`(stdout: String, stderr: String) {
        world.lastResult = world.verifier.verify(stdout, stderr)
    }

    @Then("the verdict is {word}")
    fun `verdict is`(verdictName: String) {
        assertNotNull(world.lastResult)
        assertEquals(TaskVerdict.valueOf(verdictName), world.lastResult!!.verdict)
    }

    @Then("the error message is empty")
    fun `error message empty`() {
        assertNotNull(world.lastResult)
        assertEquals("", world.lastResult!!.errorMessage)
    }

    @Then("the error message contains {string}")
    fun `error message contains`(expected: String) {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult!!.errorMessage.contains(expected),
            "Expected error message to contain '$expected' but got: ${world.lastResult!!.errorMessage}")
    }
}
