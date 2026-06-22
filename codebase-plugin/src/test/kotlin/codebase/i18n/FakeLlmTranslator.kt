package codebase.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService

/**
 * Fake [TranslationService] pour tests — déterministe, sans réseau, sans clé API.
 *
 * Stratégie :
 *  - si une réponse est enregistrée via [enqueueResult], elle est renvoyée (FIFO) ;
 *  - sinon, traduction factice "[<tgt>] <sourceText>" — valide pour asserts BDD.
 *
 * Capture les requêtes reçues dans [requestsReceived] pour vérifications Cucumber.
 */
class FakeLlmTranslator : TranslationService {

    val requestsReceived = mutableListOf<TranslationRequest>()

    private val resultQueue: MutableList<TranslationResult> = mutableListOf()

    fun enqueueResult(result: TranslationResult) {
        resultQueue.add(result)
    }

    override fun translate(request: TranslationRequest): TranslationResult {
        requestsReceived.add(request)
        return if (resultQueue.isNotEmpty()) {
            resultQueue.removeAt(0)
        } else {
            TranslationResult.Success("[${request.targetLanguage}] ${request.sourceText}")
        }
    }
}