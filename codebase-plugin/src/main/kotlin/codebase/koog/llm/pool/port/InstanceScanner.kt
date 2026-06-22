package codebase.koog.llm.pool.port

import contracts.llmpool.LlmInstance

/**
 * Port secondaire : scanne les instances LLM actives sur un hôte.
 *
 * DDD/TDD : le domaine [OllamaInstanceScanner] dépend de ce port.
 * L'implémentation HTTP réelle est injectée ; un fake renvoie une liste
 * déterministe dans les tests unitaires.
 */
fun interface InstanceScanner {
    /**
     * Retourne les instances vivantes découvertes à l'adresse donnée.
     *
     * @param baseUrl URL de base sans le port (ex: "http://localhost")
     * @param port port à tester
     * @param model modèle attendu sur cette instance
     * @return l'instance si elle répond, sinon `null`
     */
    suspend fun probe(baseUrl: String, port: Int, model: String): LlmInstance?
}
