package codebase.koog.agentic

import org.gradle.api.provider.Property

/**
 * Extension DSL `codebaseGovernance` pour configurer l'ingestion de gouvernance.
 *
 * Usage DSL (build.gradle.kts) :
 * ```
 * codebaseGovernance {
 *     strictValidation.set(true)
 *     outputEnabled.set(true)
 *     reportFormat.set("json")
 * }
 * ```
 *
 * Usage CLI (priorité max) :
 * ```
 * ./gradlew ingestGovernance -Pcodebase.governance.strictValidation=true
 * ```
 */
abstract class CodebaseGovernanceExtension {
    /**
     * Fait échouer `ingestGovernance` si des chunks invalides sont détectés.
     * Défaut : `false` (rétrocompatible).
     */
    abstract val strictValidation: Property<Boolean>

    /**
     * Active l'écriture du rapport JSON sur disque.
     * Défaut : `true`.
     */
    abstract val outputEnabled: Property<Boolean>

    /**
     * Format du rapport produit.
     * Valeurs supportées : `"json"`.
     * Défaut : `"json"`.
     */
    abstract val reportFormat: Property<String>

    /**
     * Expose une snapshot immuable du DSL sous forme de modèle DDD.
     */
    fun toConfig(): GovernanceSummaryConfig = GovernanceSummaryConfig(
        strictValidation = strictValidation.getOrElse(false),
        outputEnabled = outputEnabled.getOrElse(true),
        reportFormat = reportFormat.getOrElse("json")
    )
}
