package codebase.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Step definitions pour les scénarios @epic_v6_ollama_cloud.
 *
 * Pattern PicoContainer : le [OllamaCloudWorld] est injecté par constructeur.
 * Aucun appel réseau : les instances Ollama cloud sont simulées par le pool
 * et l'adaptateur tourne quand une instance lève "quota exceeded".
 */
class OllamaCloudSteps(private val world: OllamaCloudWorld) {

    @Given("an Ollama cloud pool with {int} instances")
    fun `ollama cloud pool with N instances`(count: Int) {
        world.buildPool()
        assertEquals(count, world.pool.size(), "Pool should contain $count instances")
    }

    @Given("the first Ollama instance returns {string}")
    fun `first instance returns error`(error: String) {
        world.failingInstanceIds.add("ollama-11437")
    }

    @Given("every Ollama instance returns {string}")
    fun `every instance returns error`(error: String) {
        world.instances.forEach { world.failingInstanceIds.add(it.id) }
    }

    @When("the LLM is called through the Ollama cloud provider")
    fun `llm called through ollama cloud provider`() {
        world.callFakeLlm()
    }

    @Then("the call succeeds on the second instance")
    fun `call succeeds on second instance`() {
        assertEquals("success-ollama-11438", world.lastResult,
            "Expected fallback to second instance, got ${world.lastResult}")
        assertEquals(null, world.lastException, "No exception should be thrown")
    }

    @Then("the first instance is marked inactive or skipped")
    fun `first instance marked inactive or skipped`() {
        // L'adaptateur marque l'instance comme inactive quand l'erreur contient
        // "quota" ou "connection refused". Vérifie au moins le skip.
        assertTrue(
            world.adapter.isInactive("ollama-11437") || world.lastResult == "success-ollama-11438",
            "First instance should be inactive or skipped after quota exceeded"
        )
    }

    @Then("an IllegalStateException is thrown with message {string}")
    fun `illegal state exception thrown with message`(expectedMessage: String) {
        val exception = world.lastException
        assertNotNull(exception, "Expected an IllegalStateException")
        assertTrue(
            exception is IllegalStateException,
            "Expected IllegalStateException but got ${exception::class.simpleName}"
        )
        assertTrue(
            exception.message?.contains(expectedMessage) ?: false,
            "Expected message containing '$expectedMessage' but got '${exception.message}'"
        )
    }
}
