package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy

/**
 * Factory qui construit un [GeminiKeyPool] à partir de clés API Gemini.
 *
 * Deux sources :
 * - [fromKeys] : liste explicite de clés (DSL `geminiApiKeys` ou CLI)
 * - [fromEnvVars] : variables d'environnement `GEMINI_API_KEY_1..N`
 *
 * Chaque clé devient une [LlmInstance] avec un `baseUrl` Gemini embeddant la clé
 * en query param, un modèle Gemini multimodal, et un quota par défaut.
 */
object GeminiPoolFactory {

    private const val DEFAULT_MODEL = "gemini-2.5-flash"
    private const val ENV_PREFIX = "GEMINI_API_KEY_"
    private const val BASE_URL_TEMPLATE =
        "https://generativelanguage.googleapis.com/v1/models/{model}:generateContent?key={key}"

    fun fromKeys(
        keys: List<String>,
        model: String = DEFAULT_MODEL,
        rotationStrategy: RotationStrategy = RotationStrategy.ROUND_ROBIN,
        idPrefix: String = "gemini-key"
    ): GeminiKeyPool {
        val instances = keys.mapIndexed { index, key ->
            LlmInstance(
                id = "$idPrefix-$index",
                baseUrl = BASE_URL_TEMPLATE
                    .replace("{model}", model)
                    .replace("{key}", key),
                model = model,
                quota = QuotaConfig(resetPolicy = ResetPolicy.NEVER)
            )
        }
        return GeminiKeyPool(instances, rotationStrategy)
    }

    fun fromEnvVars(
        env: Map<String, String>,
        model: String = DEFAULT_MODEL,
        rotationStrategy: RotationStrategy = RotationStrategy.ROUND_ROBIN
    ): GeminiKeyPool {
        val keys = (1..MAX_ENV_INDEX)
            .mapNotNull { n -> env["${ENV_PREFIX}$n"]?.takeIf { it.isNotBlank() } }
        return fromKeys(keys, model, rotationStrategy)
    }

    private const val MAX_ENV_INDEX = 100
}