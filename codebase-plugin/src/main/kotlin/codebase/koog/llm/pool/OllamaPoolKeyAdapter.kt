package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.LlmInstancePool

/**
 * Wrapper résilient autour d'un [LlmInstancePool] Ollama.
 *
 * Responsabilités :
 * - Protéger le pool de clés contre les erreurs transitoires (quota, connexion refusée).
 * - Tourner proactivement vers l'instance suivante quand l'appel échoue.
 * - Marquer une instance comme inactive quand elle refuse la connexion.
 * - Lever [IllegalStateException] explicite quand le pool est épuisé.
 *
 * Ce wrapper ne contient pas de logique LLM — il délègue l'appel via [call].
 */
class OllamaPoolKeyAdapter(
    private val pool: LlmInstancePool
) : LlmInstancePool by pool {

    private val inactive = mutableSetOf<String>()

    /**
     * Retourne la prochaine instance active, en excluant celles marquées inactives
     * et celles dont le quota est dépassé. Si aucune instance n'est disponible,
     * lève [IllegalStateException].
     */
    override fun nextInstance(): LlmInstance {
        if (pool.size() == 0) {
            throw IllegalStateException("Ollama pool is empty — no instances configured")
        }

        val candidates = pool.instances()
            .filter { it.id !in inactive }
            .filter { !pool.isQuotaExceeded(it) }

        if (candidates.isEmpty()) {
            throw IllegalStateException("All Ollama instances are unavailable (inactive or quota exceeded)")
        }

        // Utilise le pool sous-jacent pour la rotation, mais force le skip
        // des instances inactives en consommant discrètement nextInstance jusqu'à
        // obtenir une instance autorisée. On limite les tentatives au nombre total
        // d'instances pour éviter une boucle infinie si le pool sous-jacent est corrompu.
        repeat(pool.size()) {
            val candidate = pool.nextInstance()
            if (candidate.id in inactive || pool.isQuotaExceeded(candidate)) {
                return@repeat
            }
            return candidate
        }

        // Fallback déterministe si la rotation du pool ne fournit pas d'instance valide
        return candidates.first()
    }

    /**
     * Exécute [block] sur l'instance fournie par [nextInstance].
     * En cas d'erreur liée au quota ou à la connexion, marque l'instance et
     * retente sur l'instance suivante jusqu'à épuisement du pool.
     *
     * @param block appel LLM à exécuter
     * @return résultat de [block]
     * @throws IllegalStateException si toutes les instances sont épuisées
     */
    fun <T> callWithRotation(block: (LlmInstance) -> T): T {
        val errors = mutableListOf<Pair<String, String>>()
        repeat(pool.size()) {
            val instance = try {
                nextInstance()
            } catch (e: IllegalStateException) {
                throw buildExhaustedException(errors, e)
            }

            try {
                return block(instance)
            } catch (e: Exception) {
                val message = e.message ?: "unknown"
                errors.add(instance.id to message)
                when {
                    isQuotaError(message) -> {
                        // Le pool sous-jacent a déjà comptabilisé l'appel ; on continue.
                    }

                    isConnectionError(message) -> {
                        inactive.add(instance.id)
                    }

                    else -> throw e
                }
                // Continue la boucle pour tenter l'instance suivante
            }
        }

        throw buildExhaustedException(errors)
    }

    /** Réactive toutes les instances précédemment marquées inactives. */
    fun resetInactive() {
        inactive.clear()
    }

    /** True si l'instance est marquée inactive. */
    fun isInactive(instanceId: String): Boolean = instanceId in inactive

    private fun isQuotaError(message: String): Boolean =
        message.contains("quota", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("exceeded", ignoreCase = true) ||
            message.contains("429", ignoreCase = true)

    private fun isConnectionError(message: String): Boolean =
        message.contains("connection refused", ignoreCase = true) ||
            message.contains("refused", ignoreCase = true) ||
            message.contains("connect", ignoreCase = true) ||
            message.contains("unreachable", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("socket", ignoreCase = true)

    private fun buildExhaustedException(
        errors: List<Pair<String, String>>,
        cause: Throwable? = null
    ): IllegalStateException {
        val summary = if (errors.isEmpty()) {
            "No Ollama instance could handle the request"
        } else {
            errors.joinToString(
                prefix = "All Ollama instances failed: ",
                separator = "; "
            ) { "${it.first}: ${it.second}" }
        }
        return IllegalStateException(summary, cause)
    }
}
