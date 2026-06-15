package codebase.koog.llm

/**
 * Fake provider LLM pour les tests — sans clé API, sans réseau.
 * Retourne des réponses déterministes pour valider le pipeline BDD.
 */
class FakeLlmProvider : LlmProvider {

    val promptsReceived = mutableListOf<String>()

    var nextResponse: String = "I'll execute: add_dark_mode_toggle"

    private val responseQueue: MutableList<String> = mutableListOf()

    fun enqueueResponse(response: String) {
        responseQueue.add(response)
    }

    override suspend fun call(prompt: String): String {
        promptsReceived.add(prompt)
        return if (responseQueue.isNotEmpty()) {
            responseQueue.removeAt(0)
        } else {
            nextResponse
        }
    }
}
