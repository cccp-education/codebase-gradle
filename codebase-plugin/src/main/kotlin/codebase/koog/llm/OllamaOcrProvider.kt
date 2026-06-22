package codebase.koog.llm

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

class OllamaOcrProvider(
    private val baseUrl: String = "http://localhost:11437",
    private val model: String = "gpt-oss:120b-cloud",
    private val timeoutSeconds: Long = 120
) : VisionProvider {

    private val log = LoggerFactory.getLogger(OllamaOcrProvider::class.java)
    private val objectMapper = ObjectMapper()

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        log.info(
            "[OllamaOcr] Processing image: mimeType={}, language={}, model={}, " +
            "maxTokens={}, imageSize={}bytes, baseUrl={}",
            mimeType, language, model, maxTokens, imageBytes.size, baseUrl
        )

        val ocrPrompt = buildOcrPrompt(language, model)
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)

        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to ocrPrompt,
                    "images" to listOf(base64Image)
                )
            ),
            "stream" to false,
            "options" to mapOf(
                "num_predict" to maxTokens
            )
        )

        val jsonBody = objectMapper.writeValueAsString(requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .build()

        return try {
            val response = withContext(Dispatchers.IO) {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            }

            if (response.statusCode() != 200) {
                throw IllegalStateException(
                    "Ollama OCR failed: HTTP ${response.statusCode()} — ${response.body()}"
                )
            }

            val responseBody = objectMapper.readTree(response.body())
            val content = responseBody["message"]?.get("content")?.asText()
                ?: throw IllegalStateException("Ollama OCR response missing message.content")

            log.info("[OllamaOcr] Response received: length={}", content.length)
            content
        } catch (e: Exception) {
            throw IllegalStateException(
                "Ollama OCR failed for model=$model at $baseUrl: ${e.message}", e
            )
        }
    }

    private fun buildOcrPrompt(language: String, model: String): String {
        val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return """
            Tu es un expert OCR multilingue (32 langues supportées). Analyse l'image ci-dessous et extrait TOUT le texte visible.
            Langue source : $language
            Format de sortie : AsciiDoc structuré avec :
            - Titres de section (== Titre)
            - Paragraphes
            - Tableaux (|=== ... |===) si présents
            - Listes à puces (* item)
            - Métadonnées en commentaire AsciiDoc (// OCR par Ollama Vision, modèle: $model, date: $date)

            RÈGLES :
            - Ne pas inventer de texte qui n'est pas dans l'image
            - Si l'image est floue ou illisible, indiquer "[OCR] Zone illisible" en commentaire
            - Conserver la mise en page logique (colonnes, tableaux)
            - Indiquer la confiance estimée en commentaire (// Confiance: haute/moyenne/basse)
            - Détecter automatiquement la langue si le texte n'est pas en $language
        """.trimMargin()
    }
}
