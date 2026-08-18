package codebase.finetuning

/**
 * Port interne du registre Ollama — expose les deux opérations
 * nécessaires au fine-tuning N1 (EPIC FT-PIPELINE US-2).
 *
 *  * [createModel] — `POST /api/create` : crée un modèle dans le
 *    registre Ollama local à partir d'un Modelfile (base model +
 *    continual pre-training corpus).
 *  * [pushModel] — `POST /api/push` : pousse le modèle vers le
 *    registry distant (cloud Ollama Pro).
 *
 * Domaine Gradle-free / coroutine-free — synchronous contract pour
 * unit-testability (pattern [FineTuningPipeline], `TranscriptLlmEnhancer`).
 * L'implémentation production [OllamaHttpRegistryClient] ponte vers
 * `java.net.http.HttpClient` ; les tests injectent un stub.
 *
 * @see OllamaFineTunerAdapter
 */
interface OllamaRegistryClient {

    /**
     * Crée un modèle dans le registre Ollama à partir du Modelfile
     * encapsulé dans [request].
     *
     * @return [RegistryResponse.ok] si la création a réussi (HTTP 2xx),
     *         [RegistryResponse.fail] sinon (HTTP 4xx/5xx).
     */
    fun createModel(request: CreateModelRequest): RegistryResponse

    /**
     * Pousse le modèle [request.modelName] vers le registry distant.
     *
     * @return [RegistryResponse.ok] si le push a réussi (HTTP 2xx),
     *         [RegistryResponse.fail] sinon (HTTP 4xx/5xx).
     */
    fun pushModel(request: PushModelRequest): RegistryResponse
}

/**
 * Requête de création de modèle — `POST /api/create`.
 *
 * @param modelName    nom du modèle à créer dans le registre Ollama.
 * @param modelfile    contenu du Modelfile (FROM baseModel + ADAPTER
 *        + TEMPLATE + PARAMETER…).
 * @param stream       mode stream (false pour réponse synchrone).
 */
data class CreateModelRequest(
    val modelName: String,
    val modelfile: String,
    val stream: Boolean = false
)

/**
 * Requête de push — `POST /api/push`.
 *
 * @param modelName nom du modèle local à pousser vers le registry.
 * @param stream    mode stream (false pour réponse synchrone).
 */
data class PushModelRequest(
    val modelName: String,
    val stream: Boolean = false
)

/**
 * Réponse du registry Ollama — sealed DDD (pattern `FineTuningResult`).
 *
 *  * [Ok] — HTTP 2xx, opération réussie.
 *  * [Fail] — HTTP 4xx/5xx, opération échouée (statusCode + body).
 */
sealed interface RegistryResponse {
    val isOk: Boolean

    data class Ok(val statusCode: Int = 200, val body: String = "{}") : RegistryResponse {
        override val isOk: Boolean get() = true
    }

    data class Fail(val statusCode: Int, val body: String) : RegistryResponse {
        override val isOk: Boolean get() = false

        companion object {
            fun of(statusCode: Int, body: String): Fail = Fail(statusCode, body)
        }
    }

    companion object {
        fun ok(statusCode: Int = 200, body: String = "{}"): RegistryResponse = Ok(statusCode, body)
        fun fail(statusCode: Int, body: String): RegistryResponse = Fail(statusCode, body)
    }
}