package codebase.finetuning

/**
 * Requête de fine-tuning N1 — données d'entrée du pipeline
 * (EPIC FT-PIPELINE US-2, domaine `codebase.finetuning`).
 *
 * Domaine pur (pas de Gradle, pas de coroutine) — unit-testable avec
 * un fake. L'adapter [OllamaFineTunerAdapter] pont vers l'API Ollama
 * vit hors du domaine.
 *
 * @param baseModel       nom du modèle de base dans le registre Ollama
 *        (non-blank). Ex: `gpt-oss:120b-cloud`, `gemma4:31b-cloud`.
 * @param dataset         globs ou chemins du corpus de fine-tuning
 *        (non-empty). Ex: `["docs/afnor/**/*.adoc"]`.
 * @param outputModelName nom du modèle fine-tuné à enregistrer dans
 *        Ollama (non-blank). Ex: `expert-cda`.
 * @param corpusRatio     ratio du corpus cible réinjecté en continual
 *        pre-training (0.0..1.0, défaut `0.10` — méthode ACL 2024
 *        arXiv 2311.08545, référencée `BenchmarkRunner.kt:195`).
 */
data class FineTuningRequest(
    val baseModel: String,
    val dataset: List<String>,
    val outputModelName: String,
    val corpusRatio: Double = 0.10
) {
    init {
        require(baseModel.isNotBlank()) { "baseModel must not be blank" }
        require(dataset.isNotEmpty()) { "dataset must not be empty" }
        require(outputModelName.isNotBlank()) { "outputModelName must not be blank" }
        require(corpusRatio in 0.0..1.0) {
            "corpusRatio must be in [0.0, 1.0], got $corpusRatio"
        }
    }
}