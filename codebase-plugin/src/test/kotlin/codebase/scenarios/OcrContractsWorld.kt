package codebase.scenarios

import codebase.koog.llm.VisionProvider
import java.io.File

/**
 * Shared world for `@ocr-contracts` scenarios (EPIC CDX-OCR-CONTRACTS US-4).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 *  - the [visionStub] fake AI provider backing the real
 *    [codebase.koog.llm.adapter.VisionOcrEngineAdapter] (N0 port impl);
 *  - the last [ocrResult] produced through the N0 contract;
 *  - the Gradle-task side state (scan file + error) for the degraded
 *    Tesseract boundary scenarios.
 *
 * Pattern `RagSocleWorld` (PicoContainer-scoped, one fresh instance per
 * scenario via the `@Given` init step).
 */
class OcrContractsWorld {

    val visionStub = StubVisionProvider()
    var ocrResult: contracts.ocr.OcrResult? = null
    var engine: contracts.ocr.OcrEngine? = null

    var scanFile: File? = null
    var taskError: Exception? = null
    var outputDir: File? = null
}

/**
 * Test-only [VisionProvider] stub — returns canned AsciiDoc responses
 * (popped in order, the last one repeated) and records call count +
 * last arguments, without touching Gemini, Ollama or the network
 * (pattern `RecordingVisionProvider` + FIFO `FakeFineTuningLlm`).
 */
class StubVisionProvider(
    responses: List<String> = listOf("= Structured Output")
) : VisionProvider {

    private val queue = ArrayDeque(responses)

    fun resetQueue(responses: List<String>) {
        queue.clear()
        queue.addAll(responses)
    }

    var callCount: Int = 0
        private set
    var lastImageBytes: ByteArray? = null
        private set
    var lastMimeType: String? = null
        private set
    var lastLanguage: String? = null
        private set

    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        callCount++
        lastImageBytes = imageBytes
        lastMimeType = mimeType
        lastLanguage = language
        val response = queue.removeFirstOrNull() ?: queue.lastOrNull()
        return response ?: "= Structured Output"
    }
}
