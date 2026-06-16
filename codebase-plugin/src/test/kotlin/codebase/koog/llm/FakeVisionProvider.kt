package codebase.koog.llm

import codebase.koog.llm.pool.GeminiKeyPool
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * Fake Vision Provider for tests — zero network call.
 *
 * Same signature as [GeminiVisionProvider.processImage] but returns
 * a deterministic structured AsciiDoc snippet built from the input metadata.
 *
 * Uses [GeminiKeyPool] internally to simulate key rotation, quota exceeded
 * detection, and fallback behavior — validating the OCR pipeline plumbing
 * without a real Gemini API key.
 *
 * @param keyPool GeminiKeyPool with test secrets (default: 2 keys, quota 10, threshold 50%)
 */
class FakeVisionProvider(
    val keyPool: GeminiKeyPool = GeminiKeyPool(
        listOf(
            LlmInstance(
                id = "test-gemini-key-1",
                baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=test-secret-1",
                model = "gemini-2.5-flash",
                quota = QuotaConfig(limitValue = 10, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER)
            ),
            LlmInstance(
                id = "test-gemini-key-2",
                baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=test-secret-2",
                model = "gemini-2.5-flash",
                quota = QuotaConfig(limitValue = 10, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER)
            )
        ),
        rotationStrategy = RotationStrategy.ROUND_ROBIN
    )
) : VisionProvider {
    private val log = LoggerFactory.getLogger(FakeVisionProvider::class.java)

    private var _lastUsedKeyId: String? = null
    val lastUsedKeyId: String? get() = _lastUsedKeyId

    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        val instance = keyPool.nextInstance()
        _lastUsedKeyId = instance.id
        log.info(
            "[FakeVision] key={}, model={}, mimeType={}, size={}bytes, poolSize={}",
            instance.id, instance.model, mimeType, imageBytes.size, keyPool.size()
        )
        val encodedPreview = Base64.getEncoder().encodeToString(imageBytes.take(16).toByteArray())
        val langLabel = when (language.lowercase()) {
            "fr" -> "Titre Principal"
            "en" -> "Main Title"
            "de" -> "Haupttitel"
            else -> "Title"
        }
        return """
            = $langLabel
            // OCR par FakeVisionProvider (test)
            // Gemini key: ${instance.id}
            // Pool size: ${keyPool.size()} keys
            // Confiance: haute (mock)
            // Modèle mocké: $model
            // Langue: $language
            // Taille image: ${imageBytes.size} bytes
            // MIME: $mimeType
            // Preview base64: $encodedPreview

            == Section 1

            Ceci est un paragraphe extrait automatiquement par le moteur OCR factice.
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
