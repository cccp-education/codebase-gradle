package codebase.koog.llm

import org.slf4j.LoggerFactory
import java.util.Base64

class FakeOllamaOcrProvider : VisionProvider {
    private val log = LoggerFactory.getLogger(FakeOllamaOcrProvider::class.java)

    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        log.info("[FakeOllamaOcr] Returning fake OCR for mimeType={}, size={}bytes, model={}", mimeType, imageBytes.size, model)
        val encodedPreview = Base64.getEncoder().encodeToString(imageBytes.take(16).toByteArray())
        val langLabel = when (language.lowercase()) {
            "fr" -> "Titre Principal"
            "en" -> "Main Title"
            "de" -> "Haupttitel"
            else -> "Title"
        }
        return """
            = $langLabel
            // OCR par FakeOllamaOcrProvider (test)
            // Confiance: haute (mock)
            // Modèle mocké: $model
            // Langue: $language
            // Taille image: ${imageBytes.size} bytes
            // MIME: $mimeType
            // Preview base64: $encodedPreview

            == Section 1

            Ceci est un paragraphe extrait automatiquement par le moteur OCR Ollama factice.
            Il simule un résultat structuré sans appel réseau.

            == Section 2

            * Premier élément de liste
            * Deuxième élément
            * Troisième avec des **caractères gras**

            |===
            | Colonne A | Colonne B
            | Valeur 1  | Valeur 2
            | Valeur 3  | Valeur 4
            |===
        """.trimIndent()
    }
}
