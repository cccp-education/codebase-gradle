package codebase.finetuning

/**
 * Résultat d'un fine-tuning — sealed DDD (EPIC FT-PIPELINE US-2).
 *
 * Deux variantes mutuellement exclusives :
 *  * [Success] — modèle fine-tuné produit (GGUF) + score de validation.
 *  * [Failure] — pipeline dégradé : Ollama indisponible, erreur
 *    entraînement, etc. Le dataset original est préservé (economy of
 *    ink : un échec n'est pas un crash, le caller garde un état valide).
 *
 * Pattern `ValidationResult` (slider SLD-8.3, capsule `ValidationResult`).
 * Domaine pur (pas de Gradle/coroutine) — unit-testable avec fakes.
 */
sealed interface FineTuningResult {

    val isSuccess: Boolean
    val isFailure: Boolean

    /**
     * Fine-tuning réussi — le modèle GGUF est produit, poussé vers
     * Ollama registry, et enregistré sous [outputModelName].
     *
     * @param outputModelName  nom du modèle enregistré dans Ollama.
     * @param ggufPath         chemin local du fichier GGUF exporté.
     * @param iterations       nombre d'itérations effectuées
     *        (cycle itératif `FineTuningGraph`, US-3 future).
     * @param validationScore  score de qualité du modèle fine-tuné
     *        (perplexity, human-eval, expert-eval — 0.0..1.0).
     */
    data class Success(
        val outputModelName: String,
        val ggufPath: String,
        val iterations: Int,
        val validationScore: Double
    ) : FineTuningResult {
        init {
            require(outputModelName.isNotBlank()) { "outputModelName must not be blank" }
            require(iterations > 0) { "iterations must be positive, got $iterations" }
            require(validationScore in 0.0..1.0) {
                "validationScore must be in [0.0, 1.0], got $validationScore"
            }
        }

        override val isSuccess: Boolean get() = true
        override val isFailure: Boolean get() = false
    }

    /**
     * Fine-tuning dégradé — Ollama indisponible, erreur d'entraînement,
     * ou validation en dessous du seuil. Le [originalDataset] est
     * préservé intact pour retry ou usage manuel.
     *
     * @param reason          cause du degraded mode.
     * @param originalDataset dataset d'entrée original (non-transformé).
     */
    data class Failure(
        val reason: String,
        val originalDataset: List<String>
    ) : FineTuningResult {
        init {
            require(reason.isNotBlank()) { "reason must not be blank" }
        }

        override val isSuccess: Boolean get() = false
        override val isFailure: Boolean get() = true
    }

    companion object {
        /** Raccourci factory — succès avec score 1.0 et 1 itération. */
        fun success(
            outputModelName: String,
            ggufPath: String,
            iterations: Int = 1,
            validationScore: Double = 1.0
        ): FineTuningResult = Success(outputModelName, ggufPath, iterations, validationScore)

        /** Raccourci factory — degraded avec dataset préservé. */
        fun failure(reason: String, originalDataset: List<String>): FineTuningResult =
            Failure(reason, originalDataset)
    }
}