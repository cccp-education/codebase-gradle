package codebase.koog.llm

import codebase.koog.llm.pool.FakeEnvironmentReader
import codebase.koog.llm.pool.FakeInstanceScanner
import codebase.koog.llm.pool.OllamaInstanceScanner
import codebase.koog.llm.pool.OllamaLlmProvider
import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.koog.llm.pool.port.InstanceScanner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

/**
 * Tests unitaires pour LlmProviderResolver — mapping model → provider.
 *
 * Architecture TDD : ces tests définissent le comportement attendu.
 * - "gemini" → GeminiLlmProvider (lazy, sans appel .call())
 * - "ollama", "deepseek", "" → OllamaLlmProvider avec gpt-oss:120b-cloud
 *   (pool alimenté par scanner quand OLLAMA_POOL_PORTS absent)
 * - Autre chaîne → OllamaLlmProvider avec cette chaîne comme model name
 */
class LlmProviderResolverTest {

    @AfterEach
    fun resetScannerFactory() {
        LlmProviderResolver.scannerFactory = { OllamaInstanceScanner() }
    }

    @Test
    fun `resolve gemini returns GeminiLlmProvider`() {
        val provider = LlmProviderResolver.resolve("gemini")
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolve GEMINI uppercase returns GeminiLlmProvider`() {
        val provider = LlmProviderResolver.resolve("GEMINI")
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolve ollama returns OllamaLlmProvider`() {
        injectFakeScanner(livePorts = setOf(11437, 11438))
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve deepseek returns OllamaLlmProvider`() {
        injectFakeScanner(livePorts = setOf(11437))
        val provider = LlmProviderResolver.resolve("deepseek")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve blank returns OllamaLlmProvider as default`() {
        injectFakeScanner(livePorts = setOf(11437))
        val provider = LlmProviderResolver.resolve("")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve unknown model returns OllamaLlmProvider`() {
        val provider = LlmProviderResolver.resolve("custom-model:latest")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve ollama uses OLLAMA_POOL_PORTS when set`() {
        injectFakeScanner(
            livePorts = setOf(11437, 11438, 11450),
            env = mapOf("OLLAMA_POOL_PORTS" to "11450")
        )
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
    }

    private fun injectFakeScanner(
        livePorts: Set<Int>,
        env: Map<String, String> = emptyMap()
    ) {
        val scanner = object : InstanceScanner {
            private val delegate = FakeInstanceScanner(livePorts)
            override suspend fun probe(baseUrl: String, port: Int, model: String) =
                delegate.probe(baseUrl, port, model)
        }
        val envReader = FakeEnvironmentReader(env)
        LlmProviderResolver.scannerFactory = {
            OllamaInstanceScanner(scanner, envReader)
        }
    }
}
