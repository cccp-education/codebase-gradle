package codebase.koog.llm.pool.port

/**
 * Port secondaire : lecture des variables d'environnement.
 *
 * Permet de substituer un fake dans les tests sans muter `System.getenv`.
 */
fun interface EnvironmentReader {
    fun get(name: String): String?
}
