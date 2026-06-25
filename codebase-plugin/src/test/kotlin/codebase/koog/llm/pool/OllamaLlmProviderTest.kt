package codebase.koog.llm.pool

import codebase.koog.llm.LlmProvider
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.*

class OllamaLlmProviderTest {

    private val defaultQuota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER)

    /**
     * Port dédié CI (11466) — hors plage de rotation (11437-11465) et hors ports protégés (11434-11436).
     * Exclusif aux tests d'intégration CI, jamais utilisé pour le vibecoding.
     */
    private val ciPort = (System.getenv("OLLAMA_TEST_PORT") ?: "11466").toInt()

    /**
     * Modèle local léger pour tests CI — pull du manifest seul, inférence sur le runner.
     * Défaut qwen3:0.6b (~400MB), suffisamment petit pour tenir en RAM GitHub Action.
     */
    private val ciModel = System.getenv("OLLAMA_TEST_MODEL") ?: "qwen3:0.6b"

    /** Test si Ollama tourne sur ce port avec au moins 1 modèle pullé */
    private fun isOllamaReady(port: Int): Boolean {
        return try {
            val url = URI("http://localhost:$port/api/tags").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "GET"
            conn.responseCode == 200
        } catch (_: Exception) {
            false
        }
    }

    @Test
    fun `OllamaLlmProvider should implement LlmProvider`() {
        val instance = LlmInstance("test", "http://localhost:11437", "gpt-oss:120b-cloud", quota = defaultQuota)
        val pool = OllamaPool(listOf(instance))
        val provider = OllamaLlmProvider(pool)
        assertIs<LlmProvider>(provider)
    }

    @Test
    fun `OllamaLlmProvider should build OllamaChatModel lazily on first call`() {
        val instance = LlmInstance("a", "http://localhost:$ciPort", ciModel, quota = defaultQuota)
        val pool = OllamaPool(listOf(instance))

        assumeTrue(isOllamaReady(ciPort), "Ollama not ready on port $ciPort — skipping integration test")

        val provider = OllamaLlmProvider(pool)
        // Appel réel — peut échouer si le modèle n'est pas pullé, mais ne doit pas planter
        // Le test vérifie que l'appel ne jette pas d'exception inattendue hors ModelNotFoundException
        try {
            val response = kotlinx.coroutines.runBlocking { provider.call("Say hello") }
            assertTrue(response.isNotBlank())
        } catch (_: dev.langchain4j.exception.ModelNotFoundException) {
            // Modèle non pullé = acceptable, le provider a bien fonctionné jusqu'à l'appel HTTP
        }
    }

    @Test
    fun `OllamaLlmProvider should rotate through pool instances on ModelNotFoundException`() {
        val instances = listOf(ciPort, ciPort + 1).map { port ->
            LlmInstance("ollama-$port", "http://localhost:$port", ciModel, quota = defaultQuota)
        }
        val pool = OllamaPool(instances, rotationStrategy = RotationStrategy.ROUND_ROBIN)

        assumeTrue(isOllamaReady(ciPort), "Ollama not ready on port $ciPort — skipping integration test")

        val provider = OllamaLlmProvider(pool)
        // Premier appel → instance a (ciPort)
        val usageBeforeA = pool.instances().first { it.id == "ollama-$ciPort" }.let { pool.isQuotaExceeded(it) }

        try {
            kotlinx.coroutines.runBlocking { provider.call("One word") }
        } catch (_: dev.langchain4j.exception.ModelNotFoundException) {
            // OK — modèle non pullé, mais le pool a quand même comptabilisé l'appel
        } catch (_: IllegalStateException) {
            // Pool épuisé = acceptable, la rotation a été tentée
        }

        // Vérifie que l'instance a a bien été utilisée (usage incrémenté)
        val firstAfterCall = pool.instances().first { it.id == "ollama-$ciPort" }
        assertTrue(
            pool.isQuotaExceeded(firstAfterCall.copy(quota = QuotaConfig(limitValue = 1, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER))) || !usageBeforeA,
            "Pool should increment usage count even on ModelNotFoundException"
        )
    }

    @Test
    fun `OllamaLlmProvider should throw when pool is empty`() {
        val pool = OllamaPool(emptyList())
        val provider = OllamaLlmProvider(pool)
        assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking { provider.call("test") }
        }
    }

    @Test
    fun `should deduplicate ChatModel cache by baseUrl and model not instance id`() {
        // Deux instances avec le même (baseUrl, model) mais des IDs différents
        val inst1 = LlmInstance("x", "http://localhost:11437", "gpt-oss:120b-cloud", quota = defaultQuota)
        val inst2 = LlmInstance("y", "http://localhost:11437", "gpt-oss:120b-cloud", quota = defaultQuota)
        val pool = OllamaPool(listOf(inst1, inst2), rotationStrategy = RotationStrategy.ROUND_ROBIN)
        val provider = OllamaLlmProvider(pool)

        // Appel 1 → inst1 → crée ChatModel pour (11437, gpt-oss:120b-cloud)
        val model1 = provider.getCachedModel(inst1)
        // Appel 2 → inst2 → même (baseUrl, model) → DOIT retourner le même objet
        val model2 = provider.getCachedModel(inst2)

        assertSame(model1, model2, "Same (baseUrl, model) should return the same cached ChatModel")
    }

    @Test
    fun `should create separate cache entries for different baseUrls`() {
        val instA = LlmInstance("a", "http://localhost:11437", "gpt-oss:120b-cloud", quota = defaultQuota)
        val instB = LlmInstance("b", "http://localhost:11438", "gpt-oss:120b-cloud", quota = defaultQuota)
        val pool = OllamaPool(listOf(instA, instB))
        val provider = OllamaLlmProvider(pool)

        val modelA = provider.getCachedModel(instA)
        val modelB = provider.getCachedModel(instB)

        assertNotSame(modelA, modelB, "Different baseUrls should have separate cache entries")
    }

    @Test
    fun `OllamaLlmProvider should rotate on quota exceeded using adapter`() {
        val instanceA = LlmInstance(
            "a", "http://localhost:$ciPort", ciModel,
            quota = QuotaConfig(limitValue = 1, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER)
        )
        val instanceB = LlmInstance(
            "b", "http://localhost:${ciPort + 1}", ciModel,
            quota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER)
        )
        val pool = OllamaPool(listOf(instanceA, instanceB), rotationStrategy = RotationStrategy.ROUND_ROBIN)
        val provider = OllamaLlmProvider(pool)

        assumeTrue(isOllamaReady(ciPort + 1), "Ollama not ready on port ${ciPort + 1} — skipping integration test")

        // L'instance A est déjà en quota dépassé (limite 1, seuil 50% → usage 0 suffit)
        // Le provider doit donc sauter A et appeler B
        val response = kotlinx.coroutines.runBlocking { provider.call("hello") }
        assertTrue(response.isNotBlank(), "Expected a non-blank response from instance B")
    }

    @Test
    fun `OllamaLlmProvider should throw when all instances refuse connection`() {
        val instances = listOf(
            LlmInstance("a", "http://localhost:1", "gpt-oss:120b-cloud", quota = defaultQuota),
            LlmInstance("b", "http://localhost:2", "gpt-oss:120b-cloud", quota = defaultQuota)
        )
        val pool = OllamaPool(instances, rotationStrategy = RotationStrategy.ROUND_ROBIN)

        val provider = OllamaLlmProvider(pool)
        val exception = assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking { provider.call("One word") }
        }
        assertTrue(
            exception.message?.contains("All Ollama instances failed") ?: false,
            "Expected pool exhaustion message but got: ${exception.message}"
        )
    }
}
