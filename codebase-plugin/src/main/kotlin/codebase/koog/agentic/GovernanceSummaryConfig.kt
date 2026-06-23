package codebase.koog.agentic

/**
 * Configuration DDD du summary d'ingestion de gouvernance.
 *
 * Port pur (Pas de Gradle) : peut être instancié et testé sans Project/Task.
 * Valeurs par défaut rétrocompatibles : strictValidation désactivé.
 */
data class GovernanceSummaryConfig(
    val strictValidation: Boolean = false,
    val outputEnabled: Boolean = true,
    val reportFormat: String = "json",
    val incremental: Boolean = false,
    val chunkIncremental: Boolean = false
)
