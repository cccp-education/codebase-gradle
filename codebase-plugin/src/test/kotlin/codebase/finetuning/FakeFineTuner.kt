package codebase.finetuning

/**
 * Fake [FineTuningPipeline] pour tests — déterministe, sans réseau,
 * sans Ollama (EPIC FT-PIPELINE US-2).
 *
 * Stratégie :
 *  * si un résultat est enregistré via [enqueueResult], il est renvoyé
 *    (FIFO — pattern `FakeLlmTranslator`);
 *  * sinon, si un résultat unique est fourni via constructeur, il est
 *    renvoyé à chaque appel;
 *  * sinon, `IllegalStateException` — aucune réponse configurée.
 *
 * Capture la dernière requête dans [lastRequest] et compte les appels
 * dans [callCount] pour vérifications unit/Cucumber.
 */
class FakeFineTuner(
    private val defaultResult: FineTuningResult? = null
) : FineTuningPipeline {

    var lastRequest: FineTuningRequest? = null
        private set

    var callCount: Int = 0
        private set

    private val resultQueue: MutableList<FineTuningResult> = mutableListOf()

    fun enqueueResult(result: FineTuningResult) {
        resultQueue.add(result)
    }

    override fun fineTune(request: FineTuningRequest): FineTuningResult {
        lastRequest = request
        callCount++
        return when {
            resultQueue.isNotEmpty() -> resultQueue.removeAt(0)
            defaultResult != null -> defaultResult
            else -> error("FakeFineTuner has no result configured")
        }
    }
}