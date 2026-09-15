package codebase.koog.llm.pool

import contracts.llmpool.RotationStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration test validating the **real** Ollama pool environment on the
 * authorized rotation range `11437..11465`.
 *
 * The unit suites ([OllamaInstanceScannerTest], [OllamaLlmProviderTest]) only
 * exercise the scanner on a single CI port and inject fake scanners elsewhere.
 * This suite closes that coverage hole: it scans the whole authorized range
 * with the real [HttpInstanceScanner] (the default of [OllamaInstanceScanner])
 * and places a real LLM call through [OllamaLlmProvider] backed by the
 * discovered pool.
 *
 * ## Gate (opt-in)
 *
 * Disabled by default — hermetic `./gradlew test` must never probe the network.
 * Set `OLLAMA_RANGE_SCAN=true` to run it against the local Docker Ollama farm:
 *
 * ```
 * OLLAMA_RANGE_SCAN=true ./gradlew test \
 *   --tests "codebase.koog.llm.pool.OllamaPoolRealEnvironmentIntegrationTest"
 * ```
 *
 * Outside that environment (CI, fresh checkout) every test skips cleanly.
 */
class OllamaPoolRealEnvironmentIntegrationTest {

    private val authorizedRange = 11437..11465

    private fun rangeScanRequested(): Boolean =
        System.getenv("OLLAMA_RANGE_SCAN")?.equals("true", ignoreCase = true) == true

    @Test
    fun `scan discovers live instances across the authorized range`() {
        assumeTrue(rangeScanRequested(), "OLLAMA_RANGE_SCAN not set — skipping real environment scan")

        val instances = runBlocking { OllamaInstanceScanner(portRange = authorizedRange).scan() }

        assertTrue(
            instances.isNotEmpty(),
            "Expected at least one live Ollama instance in $authorizedRange"
        )

        val ports = instances.map { extractPort(it.baseUrl) }
        assertTrue(
            ports.all { it in authorizedRange },
            "Discovered ports must stay within $authorizedRange but got $ports"
        )
        assertEquals(
            ports.size,
            ports.toSet().size,
            "Each live port must appear once, got $ports"
        )
    }

    @Test
    fun `provider call succeeds through the pool on the real range`() {
        assumeTrue(rangeScanRequested(), "OLLAMA_RANGE_SCAN not set — skipping real environment call")

        val instances = runBlocking { OllamaInstanceScanner(portRange = authorizedRange).scan() }
        assumeTrue(instances.isNotEmpty(), "No live Ollama instance discovered — cannot exercise the pool")

        val provider = OllamaLlmProvider(OllamaPool(instances, rotationStrategy = RotationStrategy.ROUND_ROBIN))

        val response = runBlocking { provider.call("Reply with exactly: POOL_OK") }

        assertTrue(
            response.isNotBlank(),
            "Real pool call must return a non-blank completion"
        )
    }

    private fun extractPort(baseUrl: String): Int =
        baseUrl.removePrefix("http://localhost:").toInt()
}
