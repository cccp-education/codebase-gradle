package codebase.koog.agentic

/**
 * Rapport incrémental d'une ingestion de gouvernance (V-9.19).
 *
 * Décrit les changements détectés depuis la dernière snapshot de l'état des
 * fichiers de gouvernance : fichiers ajoutés, modifiés, supprimés et inchangés.
 */
data class IncrementalReport(
    val added: List<String>,
    val modified: List<String>,
    val removed: List<String>,
    val unchanged: List<String>,
    val skippedDueToIncremental: List<String>
) {
    fun hasChanges(): Boolean = added.isNotEmpty() || modified.isNotEmpty() || removed.isNotEmpty()
}