package codebase.koog.llm

import codebase.koog.llm.pool.OllamaInstanceFactory
import codebase.koog.llm.pool.OllamaInstanceScanner
import codebase.koog.llm.pool.OllamaLlmProvider
import codebase.koog.llm.pool.OllamaPool
import codebase.koog.llm.pool.port.EnvironmentReader
import codebase.rag.GeminiConfig
import contracts.llmpool.LlmInstance
import contracts.llmpool.RotationStrategy
import kotlinx.coroutines.runBlocking

/**
 * Resolves an [LlmProvider] from a model name.
 *
 * Zero file-based configuration. Secrets live exclusively in environment
 * variables (GEMINI_API_KEY) — never in gradle.properties or any versioned file.
 *
 * Mapping:
 * - "gemini" → [GeminiLlmProvider]
 * - "ollama", "" → [OllamaLlmProvider] backed by Gemma4 Cloud pool
 *   (ROUND_ROBIN rotation across live ports discovered by [OllamaInstanceScanner]
 *   when `OLLAMA_POOL_PORTS` is absent, or from `OLLAMA_POOL_PORTS` when set)
 * - any other string → [OllamaLlmProvider] single-instance with that model name
 *
 * NOTE: blank model is handled by the caller (VibecodingTask) — no provider
 * is injected when model is empty, preserving deterministic/backward-compat mode.
 */
object LlmProviderResolver {

    private const val DEFAULT_HOST = "http://localhost:%d"
    private const val DEFAULT_PORT = 11437
    /** Modèle par défaut pour le pool Gemma4 — aligné sur OllamaPoolTest */
    private const val DEFAULT_MODEL = "gpt-oss:120b-cloud"

    /** Factory injectable pour les tests — ne jamais utiliser en production. */
    internal var scannerFactory: () -> OllamaInstanceScanner = { OllamaInstanceScanner() }

    /** Environment reader injectable pour les tests. */
    internal var environmentReader: () -> EnvironmentReader = { EnvironmentReader { System.getenv(it) } }

    fun resolve(model: String): LlmProvider {
        return when (model.lowercase().trim()) {
            "gemini" -> GeminiLlmProvider(GeminiConfig())
            "ollama", "" -> OllamaLlmProvider(
                OllamaPool(
                    resolveOllamaInstances(),
                    rotationStrategy = RotationStrategy.ROUND_ROBIN
                )
            )
            else -> {
                val port = parsePorts(
                    System.getenv("OLLAMA_BASE_URL")
                        ?.removePrefix("http://localhost:")
                        ?: DEFAULT_PORT.toString()
                ).first()
                val instance = LlmInstance(
                    id = "custom",
                    baseUrl = DEFAULT_HOST.format(port),
                    model = model
                )
                OllamaLlmProvider(OllamaPool(listOf(instance)))
            }
        }
    }

    /**
     * Détermine la source des instances Ollama par ordre de priorité :
     * 1. `OLLAMA_SCAN_PORTS=true` ou `OLLAMA_POOL_PORTS` défini -> scan actif via [scannerFactory].
     * 2. Sinon -> factory déterministe [OllamaInstanceFactory] (zéro appel réseau).
     *
     * Le scan reste synchrone car [resolve] est appelé depuis du code synchrone.
     */
    private fun resolveOllamaInstances(): List<LlmInstance> {
        val explicitPorts = environmentReader().get("OLLAMA_POOL_PORTS")
        return if (!explicitPorts.isNullOrBlank()) {
            val ports = parsePorts(explicitPorts)
            val models = OllamaInstanceFactory.AUTHORIZED_MODELS
            ports.mapIndexed { index, port ->
                LlmInstance(
                    id = "ollama-$port",
                    baseUrl = DEFAULT_HOST.format(port),
                    model = models[index % models.size],
                    volumeTag = "ollama-$port"
                )
            }
        } else if (useScanner()) {
            runBlocking { scannerFactory().scan() }
        } else {
            OllamaInstanceFactory.create()
        }
    }

    private fun useScanner(): Boolean =
        environmentReader().get("OLLAMA_SCAN_PORTS")?.equals("true", ignoreCase = true) == true

    private fun parsePorts(raw: String): List<Int> =
        raw.split(",").map { it.trim().toInt() }
}
