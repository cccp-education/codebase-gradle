package codebase.scenarios

import codebase.koog.llm.LlmProvider
import codebase.koog.llm.pool.port.EnvironmentReader
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.assertNotNull

/**
 * Cucumber step definitions for SLD-8 US-8.1 — LlmBuildService Gradle bridge.
 *
 * Validates that [codebase.koog.llm.service.LlmBuildService] can be registered
 * via Gradle's `sharedServices.registerIfAbsent` API and that `provider()`
 * returns a non-null [LlmProvider].
 *
 * All steps are prefixed "llmService" to avoid name collisions with other
 * step classes (pattern aligned with PoolSteps).
 */
class LlmServiceSteps(private val world: LlmServiceWorld) {

    @Given("a Gradle project with sharedServices available")
    fun `gradle project with shared services`() {
        world.project = ProjectBuilder.builder().build()
        // Reset env to deterministic state (no OLLAMA_POOL_PORTS, no scanner)
        codebase.koog.llm.LlmProviderResolver.environmentReader =
            { EnvironmentReader { null } }
    }

    @When("I register LlmBuildService with model {string}")
    fun `register llm build service with model`(model: String) {
        val provider = world.project.gradle.sharedServices.registerIfAbsent(
            "llmBuildService-${model.ifBlank { "blank" }}",
            codebase.koog.llm.service.LlmBuildService::class.java
        ) { spec ->
            spec.parameters.model.set(model)
            spec.parameters.ollamaPoolPorts.set("")
            spec.maxParallelUsages.set(1)
        }
        world.serviceProvider = provider
        world.provider = provider.get().provider()
    }

    @Then("the service is instantiated successfully")
    fun `service instantiated successfully`() {
        val sp = world.serviceProvider
        assertNotNull(sp, "BuildService provider should be non-null")
        assertNotNull(sp.get(), "BuildService instance should be non-null")
    }

    @Then("the provider is non-null")
    fun `provider is non-null`() {
        assertNotNull(world.provider, "LlmProvider returned by provider() should be non-null")
    }
}