package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests unitaires baby-step pour [OllamaInstanceScanner].
 *
 * - fake HTTP : ports up/down mixtes
 * - fallback env var `OLLAMA_POOL_PORTS` prioritaire sur le scan
 * - intégration conditionnelle : ne s'exécute que si Ollama est prêt sur le port CI dédié
 */
class OllamaInstanceScannerTest {

    @Test
    fun `scan returns only live ports`() {
        val livePorts = setOf(11437, 11438, 11440)
        val scanner = OllamaInstanceScanner(FakeInstanceScanner(livePorts))

        val instances = runBlocking { scanner.scan() }

        assertEquals(3, instances.size)
        assertEquals(setOf(11437, 11438, 11440), instances.map { extractPort(it) }.toSet())
    }

    @Test
    fun `scan cycles authorized models across live ports`() {
        val livePorts = setOf(11437, 11438, 11439, 11440)
        val scanner = OllamaInstanceScanner(FakeInstanceScanner(livePorts))

        val instances = runBlocking { scanner.scan() }

        val models = instances.map { it.model }
        assertEquals(
            listOf(
                "gpt-oss:120b-cloud",
                "gemma4:31b-cloud",
                "gpt-oss:120b-cloud",
                "gemma4:31b-cloud"
            ),
            models
        )
    }

    @Test
    fun `scan includes gemma4 31b cloud in authorized models`() {
        assertTrue("gemma4:31b-cloud" in OllamaInstanceScanner.AUTHORIZED_MODELS,
            "gemma4:31b-cloud must be in the authorized models list")
    }

    @Test
    fun `scan with no live ports returns empty list`() {
        val scanner = OllamaInstanceScanner(FakeInstanceScanner(emptySet()))

        val instances = runBlocking { scanner.scan() }

        assertTrue(instances.isEmpty())
    }

    @Test
    fun `OLLAMA_POOL_PORTS env var takes priority over port range`() {
        val env = FakeEnvironmentReader(mapOf("OLLAMA_POOL_PORTS" to "11450,11451"))
        val scanner = OllamaInstanceScanner(
            FakeInstanceScanner(setOf(11450, 11451, 11437)),
            environmentReader = env
        )

        val instances = runBlocking { scanner.scan() }

        assertEquals(2, instances.size)
        assertEquals(setOf(11450, 11451), instances.map { extractPort(it) }.toSet())
    }

    @Test
    fun `parsePorts splits comma separated values`() {
        assertEquals(listOf(11437, 11438, 11439), OllamaInstanceScanner.parsePorts("11437,11438,11439"))
    }

    @EnabledIf("isOllamaReady")
    @Test
    fun `scan integration probes real Ollama on CI port`() {
        val ciPort = (System.getenv("OLLAMA_TEST_PORT") ?: "11466").toInt()
        val scanner = OllamaInstanceScanner(HttpInstanceScanner(), portRange = ciPort..ciPort)

        val instances = runBlocking { scanner.scan() }

        assertTrue(instances.isNotEmpty(), "Expected at least one live Ollama instance on port $ciPort")
        assertEquals("http://localhost:$ciPort", instances.first().baseUrl)
    }

    /**
     * Vérifie si Ollama répond sur le port CI dédié (11466 — hors plage de rotation 11437-11465,
     * hors ports protégés 11434-11436). Exclusif aux tests d'intégration CI.
     * Utilisé par [EnabledIf] pour skipper le test d'intégration quand
     * aucune instance n'est disponible.
     */
    @Suppress("unused")
    fun isOllamaReady(): Boolean {
        val ciPort = (System.getenv("OLLAMA_TEST_PORT") ?: "11466").toInt()
        return try {
            java.net.URI.create("http://localhost:$ciPort/api/tags").toURL().openStream().use { it.read() > 0 }
        } catch (_: Exception) {
            false
        }
    }

    private fun extractPort(instance: LlmInstance): Int =
        instance.baseUrl.removePrefix("http://localhost:").toInt()
}
