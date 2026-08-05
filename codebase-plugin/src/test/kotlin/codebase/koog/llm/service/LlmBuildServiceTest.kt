package codebase.koog.llm.service

import codebase.koog.llm.GeminiLlmProvider
import codebase.koog.llm.LlmProvider
import codebase.koog.llm.LlmProviderResolver
import codebase.koog.llm.pool.OllamaLlmProvider
import codebase.koog.llm.pool.port.EnvironmentReader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [LlmBuildService] — the Gradle BuildService bridge that
 * exposes [LlmProviderResolver] as an injectable Gradle service.
 *
 * Architecture TDD (baby-step RED): these tests define the expected
 * behaviour. The class [LlmBuildService] does not exist yet — compilation
 * fails until it is implemented (Step GREEN).
 *
 * Scope (US-8.1): codebase only, additive — zero modification of existing
 * code. The service delegates to [LlmProviderResolver] via the pure
 * function [LlmServiceResolver.resolveProvider], which is testable
 * without a Gradle scope.
 */
class LlmBuildServiceTest {

    @AfterEach
    fun resetResolver() {
        LlmProviderResolver.scannerFactory = { codebase.koog.llm.pool.OllamaInstanceScanner() }
        LlmProviderResolver.environmentReader = { EnvironmentReader { System.getenv(it) } }
    }

    @Test
    fun `resolveProvider returns GeminiLlmProvider for gemini model`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider: LlmProvider = LlmServiceResolver.resolveProvider("gemini")
        assertNotNull(provider)
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolveProvider returns OllamaLlmProvider for ollama model`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider: LlmProvider = LlmServiceResolver.resolveProvider("ollama")
        assertNotNull(provider)
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolveProvider returns OllamaLlmProvider for custom model name`() {
        val provider: LlmProvider = LlmServiceResolver.resolveProvider("gpt-oss:120b-cloud")
        assertNotNull(provider)
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolveProvider delegates to LlmProviderResolver with the configured model`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmServiceResolver.resolveProvider("gemini")
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolveProvider blank model falls back to ollama`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider: LlmProvider = LlmServiceResolver.resolveProvider("")
        assertNotNull(provider)
        assertIs<OllamaLlmProvider>(provider)
    }
}