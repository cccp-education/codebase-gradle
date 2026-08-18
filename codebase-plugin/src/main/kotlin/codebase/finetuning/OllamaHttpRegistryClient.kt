package codebase.finetuning

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Implémentation production de [OllamaRegistryClient] — ponte vers
 * l'API Ollama via `java.net.http.HttpClient` (EPIC FT-PIPELINE US-2).
 *
 *  * `POST /api/create` — crée un modèle dans le registre Ollama local
 *    à partir d'un Modelfile (cf. `ollama create`).
 *  * `POST /api/push` — pousse le modèle vers le registry distant
 *    (cf. `ollama push`).
 *
 * Pattern `OllamaOcrProvider` (HTTP direct JDK 24 built-in —
 * langchain4j-ollama ne couvre pas create/push).
 *
 * @param baseUrl       URL du registre Ollama local (défaut
 *        `http://localhost:11437`, premier port de la plage rotation
 *        11437-11465 — AGENTS.adoc règle ports).
 * @param timeoutSeconds timeout HTTP par requête (défaut 120s —
 *        fine-tuning lourd).
 */
class OllamaHttpRegistryClient(
    private val baseUrl: String = "http://localhost:11437",
    private val timeoutSeconds: Long = 120
) : OllamaRegistryClient {

    private val log = LoggerFactory.getLogger(OllamaHttpRegistryClient::class.java)
    private val mapper = ObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

    override fun createModel(request: CreateModelRequest): RegistryResponse =
        post("$baseUrl/api/create", mapOf(
            "name" to request.modelName,
            "modelfile" to request.modelfile,
            "stream" to request.stream
        ), op = "create")

    override fun pushModel(request: PushModelRequest): RegistryResponse =
        post("$baseUrl/api/push", mapOf(
            "name" to request.modelName,
            "stream" to request.stream
        ), op = "push")

    private fun post(uri: String, body: Map<String, Any?>, op: String): RegistryResponse {
        val json = mapper.writeValueAsString(body)
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build()
        return try {
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            if (status in 200..299) {
                log.info("[OllamaHttpRegistry] {} ok: status={}", op, status)
                RegistryResponse.ok(status, response.body())
            } else {
                log.warn("[OllamaHttpRegistry] {} fail: status={}, body={}", op, status, response.body())
                RegistryResponse.fail(status, response.body())
            }
        } catch (e: Exception) {
            log.warn("[OllamaHttpRegistry] {} error: ${e.message}", op)
            RegistryResponse.fail(-1, e.message ?: "unknown error")
        }
    }
}