package codebase.koog.llm.pool

import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.koog.llm.pool.port.InstanceScanner
import contracts.llmpool.LlmInstance

/**
 * Fake scanner déterministe pour les tests unitaires.
 *
 * Renvoie une instance vivante seulement pour les ports présents dans
 * [livePorts], avec le modèle demandé.
 */
class FakeInstanceScanner(
    private val livePorts: Set<Int>
) : InstanceScanner {

    override suspend fun probe(baseUrl: String, port: Int, model: String): LlmInstance? {
        return if (port in livePorts) {
            LlmInstance(
                id = "ollama-$port",
                baseUrl = "$baseUrl:$port",
                model = model
            )
        } else {
            null
        }
    }
}

/**
 * Fake [EnvironmentReader] pour les tests unitaires.
 */
class FakeEnvironmentReader(
    private val values: Map<String, String>
) : EnvironmentReader {
    override fun get(name: String): String? = values[name]
}
