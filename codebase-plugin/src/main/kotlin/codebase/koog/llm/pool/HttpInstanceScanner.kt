package codebase.koog.llm.pool

import codebase.koog.llm.pool.port.InstanceScanner
import contracts.llmpool.LlmInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Implémentation HTTP du port [InstanceScanner].
 *
 * Probe une instance Ollama via `GET /api/tags`.
 * Si l'endpoint répond HTTP 200, l'instance est considérée vivante.
 * Tout autre code ou exception (connexion refusée, timeout) retourne `null`.
 *
 * Timeout : 2s par probe pour ne pas bloquer le scan des 29 ports.
 */
class HttpInstanceScanner(
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
) : InstanceScanner {

    private val log = LoggerFactory.getLogger(HttpInstanceScanner::class.java)
    private val timeout = Duration.ofSeconds(2)

    override suspend fun probe(baseUrl: String, port: Int, model: String): LlmInstance? {
        val url = "$baseUrl:$port/api/tags"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .GET()
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            }
            if (response.statusCode() == 200) {
                log.debug("[HttpInstanceScanner] Live instance at {}", url)
                LlmInstance(
                    id = "ollama-$port",
                    baseUrl = "$baseUrl:$port",
                    model = model
                )
            } else {
                log.debug("[HttpInstanceScanner] Instance at {} returned HTTP {}", url, response.statusCode())
                null
            }
        } catch (e: Exception) {
            log.debug("[HttpInstanceScanner] Probe failed for {}: {}", url, e.message ?: "unknown")
            null
        }
    }
}
