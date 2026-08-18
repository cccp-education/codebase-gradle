package codebase.finetuning

/**
 * Port de fine-tuning N1 — domaine `codebase.finetuning`
 * (EPIC FT-PIPELINE US-2).
 *
 * Synchronous contract (pattern `TranscriptLlmEnhancer`, `CapsuleLlm`)
 * afin que le domaine reste Gradle-free, coroutine-free, et
 * unit-testable avec un simple fake. L'adapter [OllamaFineTunerAdapter]
 * pont vers l'API Ollama vit hors du domaine.
 *
 * Le port prend une [FineTuningRequest] (baseModel + dataset +
 * outputModelName + corpusRatio) et retourne un [FineTuningResult] :
 *  * [FineTuningResult.Success] — modèle fine-tuné produit.
 *  * [FineTuningResult.Failure] — degraded mode (Ollama unavailable,
 *    erreur entraînement), dataset original préservé.
 *
 * Fallback degraded (pattern `AudioPostProcessor.process`) :
 * l'implémentation ne lève jamais d'exception pour un échec
 * opérationnel — elle retourne `FineTuningResult.Failure` afin que
 * le caller garde un état valide (economy of ink).
 */
fun interface FineTuningPipeline {

    /**
     * Exécute un fine-tuning pour la [request].
     *
     * @return [FineTuningResult.Success] en cas de succès,
     *         [FineTuningResult.Failure] en mode degraded.
     */
    fun fineTune(request: FineTuningRequest): FineTuningResult
}