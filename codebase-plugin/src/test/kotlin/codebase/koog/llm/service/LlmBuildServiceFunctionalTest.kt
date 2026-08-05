package codebase.koog.llm.service

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * Functional tests for [LlmBuildService] — verifies the service can be
 * registered and instantiated via Gradle's `sharedServices` API, and
 * that [LlmBuildService.provider] returns a non-null [codebase.koog.llm.LlmProvider].
 *
 * Architecture TDD (baby-step RED → GREEN): the service is implemented in
 * Step 1 (unit-tested via [LlmServiceResolver]). These functional tests
 * validate the Gradle integration — BuildService registration, parameter
 * injection, and the end-to-end `provider()` call.
 *
 * Scope (US-8.1): codebase only, additive.
 */
class LlmBuildServiceFunctionalTest {

    @Test
    fun `BuildService is registrable via sharedServices`() {
        val project = ProjectBuilder.builder().build()
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "llmBuildService", LlmBuildService::class.java
        ) { spec ->
            spec.parameters.model.set("ollama")
            spec.parameters.ollamaPoolPorts.set("")
            spec.maxParallelUsages.set(1)
        }

        assertNotNull(serviceProvider, "sharedServices.registerIfAbsent should return a non-null provider")
    }

    @Test
    fun `BuildService provider returns non-null for ollama`() {
        val project = ProjectBuilder.builder().build()
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "llmBuildServiceOllama", LlmBuildService::class.java
        ) { spec ->
            spec.parameters.model.set("ollama")
            spec.parameters.ollamaPoolPorts.set("")
            spec.maxParallelUsages.set(1)
        }

        val service = serviceProvider.get()
        val provider = service.provider()
        assertNotNull(provider, "LlmBuildService.provider() must return a non-null LlmProvider")
    }

    @Test
    fun `BuildService provider returns non-null for gemini`() {
        val project = ProjectBuilder.builder().build()
        val serviceProvider = project.gradle.sharedServices.registerIfAbsent(
            "llmBuildServiceGemini", LlmBuildService::class.java
        ) { spec ->
            spec.parameters.model.set("gemini")
            spec.parameters.ollamaPoolPorts.set("")
            spec.maxParallelUsages.set(1)
        }

        val service = serviceProvider.get()
        val provider = service.provider()
        assertNotNull(provider, "LlmBuildService.provider() must return a non-null LlmProvider for gemini")
    }
}