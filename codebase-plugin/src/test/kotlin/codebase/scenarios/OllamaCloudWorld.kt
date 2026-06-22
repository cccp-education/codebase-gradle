package codebase.scenarios

import codebase.koog.llm.LlmProvider
import codebase.koog.llm.pool.OllamaPool
import codebase.koog.llm.pool.OllamaPoolKeyAdapter
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy

/**
 * World Object pour les scénarios @epic_v6_ollama_cloud et @epic_v6_resolver.
 *
 * Maintient un pool de 2 instances Ollama cloud factices et un adaptateur
 * résilient. Le "fake LLM call" est simulé par un bloc Kotlin qui lève une
 * exception sur certaines instances — zéro appel réseau.
 */
class OllamaCloudWorld {

    /** Résultat du dernier appel simulé. */
    var lastResult: String? = null

    /** Dernière exception capturée. */
    var lastException: Throwable? = null

    /** Dernier provider résolu par le resolver (EPIC V-6 resolver). */
    var lastResolvedProvider: LlmProvider? = null

    /** IDs d'instances sur lesquelles le fake appel lève "quota exceeded". */
    val failingInstanceIds = mutableSetOf<String>()

    lateinit var pool: OllamaPool
    lateinit var adapter: OllamaPoolKeyAdapter
    lateinit var instances: List<LlmInstance>

    fun buildPool() {
        instances = listOf(
            LlmInstance(
                id = "ollama-11437",
                baseUrl = "http://localhost:11437",
                model = "gpt-oss:120b-cloud",
                quota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER),
                volumeTag = "ollama-11437"
            ),
            LlmInstance(
                id = "ollama-11438",
                baseUrl = "http://localhost:11438",
                model = "gpt-oss:20b-cloud",
                quota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER),
                volumeTag = "ollama-11438"
            )
        )
        pool = OllamaPool(instances, rotationStrategy = RotationStrategy.ROUND_ROBIN)
        adapter = OllamaPoolKeyAdapter(pool)
    }

    fun callFakeLlm() {
        lastException = null
        lastResult = null
        try {
            val result = adapter.callWithRotation { instance ->
                if (instance.id in failingInstanceIds) {
                    throw RuntimeException("quota exceeded for ${instance.id}")
                }
                "success-${instance.id}"
            }
            lastResult = result
        } catch (e: IllegalStateException) {
            lastException = e
        }
    }
}
