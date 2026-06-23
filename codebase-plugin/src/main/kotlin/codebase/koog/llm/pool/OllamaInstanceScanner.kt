package codebase.koog.llm.pool

import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.koog.llm.pool.port.InstanceScanner
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig

/**
 * Scanner actif des instances Ollama disponibles sur la plage de ports
 * autorisée `11437 → 11465`.
 *
 * Règles métier :
 * - Si `OLLAMA_POOL_PORTS` est défini, il est prioritaire (mode déclaratif).
 * - Sinon, on probe HTTP `GET /api/tags` sur chaque port de la plage.
 * - Seuls les 2 modèles autorisés sont utilisés, cyclés sur les ports vivants.
 *
 * @param scanner port de probe HTTP (fake dans les tests)
 * @param environmentReader port de lecture des variables d'environnement
 * @param portRange plage de ports à scanner (défaut 11437..11465)
 */
class OllamaInstanceScanner(
    private val scanner: InstanceScanner = HttpInstanceScanner(),
    private val environmentReader: EnvironmentReader = EnvironmentReader { System.getenv(it) },
    private val portRange: IntRange = DEFAULT_PORT_RANGE
) {

    suspend fun scan(): List<LlmInstance> {
        val explicitPorts = environmentReader.get(OLLAMA_POOL_PORTS_ENV)
            ?.takeIf { it.isNotBlank() }
            ?.let { parsePorts(it) }

        val ports = explicitPorts ?: portRange.toList()
        val modelCycle = AUTHORIZED_MODELS

        val liveInstances = ports.mapIndexed { index, port ->
            val model = modelCycle[index % modelCycle.size]
            scanner.probe(DEFAULT_HOST, port, model)
        }.filterNotNull()

        return liveInstances.map { instance ->
            instance.copy(quota = DEFAULT_QUOTA)
        }
    }

    companion object {
        private const val DEFAULT_HOST = "http://localhost"
        private const val OLLAMA_POOL_PORTS_ENV = "OLLAMA_POOL_PORTS"
        private val DEFAULT_PORT_RANGE = 11437..11465
        private val DEFAULT_QUOTA = QuotaConfig()

        internal val AUTHORIZED_MODELS = listOf(
            "gpt-oss:120b-cloud",
            "gemma4:31b-cloud"
        )

        internal fun parsePorts(raw: String): List<Int> =
            raw.split(",").map { it.trim().toInt() }
    }
}
