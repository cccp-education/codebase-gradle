package codebase.finetuning

/**
 * Configuration DDD du pipeline de fine-tuning N1.
 *
 * Port pur (Pas de Gradle) : peut être instancié et testé sans Project/Task.
 * Valeurs par défaut rétrocompatibles : continual pre-training 10% du corpus
 * cible (méthode ACL 2024 arXiv 2311.08545, référencée BenchmarkRunner.kt:195).
 *
 * @param epochs                    nombre d'itérations de continual pre-training
 *        (défaut `3`).
 * @param learningRate              taux d'apprentissage (défaut `2e-4`).
 * @param batchSize                 taille de batch (défaut `4`).
 * @param corpusGlobs               globs du corpus cible (défaut vide).
 * @param continualPreTrainingRatio ratio du corpus cible réinjecté en
 *        continual pre-training (défaut `0.10`).
 */
data class FineTuningConfig(
    val epochs: Int = 3,
    val learningRate: Double = 2e-4,
    val batchSize: Int = 4,
    val corpusGlobs: List<String> = emptyList(),
    val continualPreTrainingRatio: Double = 0.10
) {
    init {
        require(epochs > 0) { "epochs must be positive, got $epochs" }
        require(learningRate > 0.0) { "learningRate must be positive, got $learningRate" }
        require(batchSize > 0) { "batchSize must be positive, got $batchSize" }
        require(continualPreTrainingRatio in 0.0..1.0) {
            "continualPreTrainingRatio must be in [0.0, 1.0], got $continualPreTrainingRatio"
        }
    }
}
