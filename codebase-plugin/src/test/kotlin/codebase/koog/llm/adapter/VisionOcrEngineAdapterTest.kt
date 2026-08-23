package codebase.koog.llm.adapter

import codebase.koog.llm.VisionProvider
import codex.ocr.OcrRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [VisionOcrEngineAdapter] — the codebase-side adapter that
 * implements the codex `OcrEngine` port by delegating to a codebase
 * `VisionProvider` (AI-assisted OCR boundary, EPIC CDX-OCR-BOUNDARY US-2).
 */
class VisionOcrEngineAdapterTest {

    private class RecordingVisionProvider(
        private val response: String = "= Structured Output"
    ) : VisionProvider {
        var lastImageBytes: ByteArray? = null
        var lastMimeType: String? = null
        var lastLanguage: String? = null
        var lastModel: String? = null
        var lastMaxTokens: Int? = null

        override suspend fun processImage(
            imageBytes: ByteArray,
            mimeType: String,
            language: String,
            model: String,
            maxTokens: Int
        ): String {
            lastImageBytes = imageBytes
            lastMimeType = mimeType
            lastLanguage = language
            lastModel = model
            lastMaxTokens = maxTokens
            return response
        }
    }

    @Test
    fun `delegates image bytes mime type and language to the vision provider`() {
        val provider = RecordingVisionProvider()
        val adapter = VisionOcrEngineAdapter(provider)
        val request = OcrRequest(
            imageData = byteArrayOf(1, 2, 3),
            format = "image/png",
            language = "fr"
        )

        adapter.process(request)

        assertTrue(provider.lastImageBytes!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertEquals("image/png", provider.lastMimeType)
        assertEquals("fr", provider.lastLanguage)
    }

    @Test
    fun `returns ocr result whose structured text comes from the provider`() {
        val adapter = VisionOcrEngineAdapter(RecordingVisionProvider(response = "= Page 1\n\nHello"))
        val request = OcrRequest(imageData = ByteArray(0), format = "image/png", language = "fr")

        val result = adapter.process(request)

        assertEquals("= Page 1\n\nHello", result.structuredText)
    }

    @Test
    fun `result metadata mirrors the request and configured model`() {
        val adapter = VisionOcrEngineAdapter(
            RecordingVisionProvider(),
            model = "gemini-2.5-pro",
            maxTokens = 4096
        )
        val request = OcrRequest(imageData = ByteArray(0), format = "image/jpeg", language = "en")

        val result = adapter.process(request)

        assertEquals("en", result.language)
        assertEquals("image/jpeg", result.sourceFormat)
        assertEquals("gemini-2.5-pro", result.model)
        assertTrue(result.confidence in 0.0..1.0)
        assertTrue(result.generatedAt.isNotBlank())
    }

    @Test
    fun `passes configured model and max tokens to the provider`() = runBlocking {
        val provider = RecordingVisionProvider()
        val adapter = VisionOcrEngineAdapter(provider, model = "gpt-oss:120b-cloud", maxTokens = 2048)
        val request = OcrRequest(imageData = ByteArray(0), format = "image/png", language = "fr")

        adapter.process(request)

        assertEquals("gpt-oss:120b-cloud", provider.lastModel)
        assertEquals(2048, provider.lastMaxTokens)
    }
}
