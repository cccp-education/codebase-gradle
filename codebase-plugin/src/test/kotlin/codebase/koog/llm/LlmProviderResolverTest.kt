package codebase.koog.llm

import codebase.koog.llm.pool.FakeEnvironmentReader
import codebase.koog.llm.pool.FakeInstanceScanner
import codebase.koog.llm.pool.OllamaInstanceFactory
import codebase.koog.llm.pool.OllamaInstanceScanner
import codebase.koog.llm.pool.OllamaLlmProvider
import codebase.koog.llm.pool.OllamaPool
import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.koog.llm.pool.port.InstanceScanner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests unitaires pour LlmProviderResolver — mapping model → provider.
 *
 * Architecture TDD : ces tests définissent le comportement attendu.
 * - "gemini" → GeminiLlmProvider (lazy, sans appel .call())
 * - "ollama", "deepseek", "" → OllamaLlmProvider avec gpt-oss:120b-cloud
 *   (pool alimenté par factory déterministe quand aucune env scanner n'est présente)
 * - Autre chaîne → OllamaLlmProvider avec cette chaîne comme model name
 */
class LlmProviderResolverTest {

    @AfterEach
    fun resetScannerFactory() {
        LlmProviderResolver.scannerFactory = { OllamaInstanceScanner() }
        LlmProviderResolver.environmentReader = { EnvironmentReader { System.getenv(it) } }
    }

    @Test
    fun `resolve gemini returns GeminiLlmProvider`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmProviderResolver.resolve("gemini")
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolve GEMINI uppercase returns GeminiLlmProvider`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmProviderResolver.resolve("GEMINI")
        assertIs<GeminiLlmProvider>(provider)
    }

    @Test
    fun `resolve ollama returns OllamaLlmProvider from deterministic factory`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
        assertPoolHasFactoryInstances(provider, count = 29)
    }

    @Test
    fun `resolve deepseek returns OllamaLlmProvider from deterministic factory`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmProviderResolver.resolve("deepseek")
        assertIs<OllamaLlmProvider>(provider)
        assertPoolHasFactoryInstances(provider, count = 29)
    }

    @Test
    fun `resolve blank returns OllamaLlmProvider from deterministic factory`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { null } }
        val provider = LlmProviderResolver.resolve("")
        assertIs<OllamaLlmProvider>(provider)
        assertPoolHasFactoryInstances(provider, count = 29)
    }

    @Test
    fun `resolve unknown model returns OllamaLlmProvider`() {
        val provider = LlmProviderResolver.resolve("custom-model:latest")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve ollama uses OLLAMA_POOL_PORTS when set`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { env ->
            if (env == "OLLAMA_POOL_PORTS") "11450" else null
        } }
        injectFakeScanner(livePorts = setOf(11450))
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
        assertPoolHasExplicitPort(provider, port = 11450)
    }

    @Test
    fun `resolve ollama falls back to scanner when OLLAMA_SCAN_PORTS is true`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { env ->
            if (env == "OLLAMA_SCAN_PORTS") "true" else null
        } }
        injectFakeScanner(livePorts = setOf(11437, 11438))
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
    }

    @Test
    fun `resolve ollama scanner fallback is case insensitive`() {
        LlmProviderResolver.environmentReader = { EnvironmentReader { env ->
            if (env == "OLLAMA_SCAN_PORTS") "TRUE" else null
        } }
        injectFakeScanner(livePorts = setOf(11437))
        val provider = LlmProviderResolver.resolve("ollama")
        assertIs<OllamaLlmProvider>(provider)
    }

    private fun assertPoolHasFactoryInstances(provider: OllamaLlmProvider, count: Int) {
        val pool = provider.pool
        assertEquals(count, pool.size(), "Pool should contain $count deterministic factory instances")
        val expected = OllamaInstanceFactory.create()
        val actual = pool.instances()
        assertEquals(expected.map { it.id }, actual.map { it.id })
        assertEquals(expected.map { it.baseUrl }, actual.map { it.baseUrl })
        assertEquals(expected.map { it.model }, actual.map { it.model })
        assertEquals(expected.map { it.volumeTag }, actual.map { it.volumeTag })
    }

    private fun assertPoolHasExplicitPort(provider: OllamaLlmProvider, port: Int) {
        val urls = provider.pool.instances().map { it.baseUrl }
        assertEquals(listOf("http://localhost:$port"), urls)
    }

    private val OllamaLlmProvider.pool: OllamaPool
        get() {
            val adapter = javaClass.getDeclaredField("adapter").apply { isAccessible = true }.get(this)
            return adapter.javaClass.getDeclaredField("pool").apply { isAccessible = true }.get(adapter) as OllamaPool
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
