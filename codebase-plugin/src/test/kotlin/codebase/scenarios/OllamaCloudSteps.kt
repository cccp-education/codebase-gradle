package codebase.scenarios

import codebase.koog.llm.LlmProviderResolver
import codebase.koog.llm.pool.OllamaInstanceFactory
import codebase.koog.llm.pool.OllamaInstanceScanner
import codebase.koog.llm.pool.OllamaLlmProvider
import codebase.koog.llm.pool.OllamaPool
import codebase.koog.llm.pool.FakeInstanceScanner
import codebase.koog.llm.pool.FakeEnvironmentReader
import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.koog.llm.pool.port.InstanceScanner
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Step definitions pour les scénarios @epic_v6_ollama_cloud et @epic_v6_resolver.
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

    // === EPIC V-6 Resolver steps ===

    @Given("no Ollama scan environment variables are set")
    fun `no ollama scan env variables`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
    }

    @Given("OLLAMA_POOL_PORTS is set to {string}")
    fun `ollama pool ports is set`(ports: String) {
        LlmProviderResolver.environmentReader = { EnvironmentReader { env ->
            if (env == "OLLAMA_POOL_PORTS") ports else null
        } }
    }

    @Given("OLLAMA_SCAN_PORTS is set to {string}")
    fun `ollama scan ports is set`(value: String) {
        LlmProviderResolver.environmentReader = { EnvironmentReader { env ->
            if (env == "OLLAMA_SCAN_PORTS") value else null
        } }
    }

    @Given("the fake scanner reports live ports {string}")
    fun `fake scanner reports live ports`(ports: String) {
        val livePorts = ports.split(",").map { it.trim().toInt() }.toSet()
        val scanner = object : InstanceScanner {
            private val delegate = FakeInstanceScanner(livePorts)
            override suspend fun probe(baseUrl: String, port: Int, model: String) =
                delegate.probe(baseUrl, port, model)
        }
        LlmProviderResolver.scannerFactory = {
            OllamaInstanceScanner(scanner, FakeEnvironmentReader(emptyMap()))
        }
    }

    @When("I resolve provider for model {string}")
    fun `resolve provider for model`(model: String) {
        world.lastResolvedProvider = LlmProviderResolver.resolve(model)
    }

    @Then("the provider is an OllamaLlmProvider")
    fun `provider is ollama llm provider`() {
        assertIs<OllamaLlmProvider>(world.lastResolvedProvider)
    }

    @Then("the provider pool contains {int} instances")
    fun `provider pool contains N instances`(count: Int) {
        val provider = world.lastResolvedProvider as OllamaLlmProvider
        assertEquals(count, provider.pool.size(), "Pool should contain $count instances")
    }

    @Then("the provider pool models cycle through the {int} authorized cloud models")
    fun `provider pool models cycle through authorized models`(count: Int) {
        val provider = world.lastResolvedProvider as OllamaLlmProvider
        val expected = OllamaInstanceFactory.AUTHORIZED_MODELS
        val actual = provider.pool.instances().take(expected.size).map { it.model }
        assertEquals(expected, actual)
        val distinctModels = provider.pool.instances().map { it.model }.toSet()
        assertEquals(count, distinctModels.size)
        for (model in OllamaInstanceFactory.AUTHORIZED_MODELS) {
            assertTrue(model in distinctModels, "Authorized model '$model' missing")
        }
    }

    @Then("the provider pool contains {int} instance on port {int}")
    fun `provider pool contains N instance on port`(count: Int, port: Int) {
        val provider = world.lastResolvedProvider as OllamaLlmProvider
        val instancesOnPort = provider.pool.instances().filter { it.baseUrl == "http://localhost:$port" }
        assertEquals(count, instancesOnPort.size, "Expected $count instance on port $port")
    }

    private val OllamaLlmProvider.pool: OllamaPool
        get() {
            val adapter = javaClass.getDeclaredField("adapter").apply { isAccessible = true }.get(this)
            return adapter.javaClass.getDeclaredField("pool").apply { isAccessible = true }.get(adapter) as OllamaPool
        }
}
