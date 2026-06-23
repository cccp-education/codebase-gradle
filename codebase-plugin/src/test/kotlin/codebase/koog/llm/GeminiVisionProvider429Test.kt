package codebase.koog.llm

import codebase.koog.llm.pool.GeminiKeyPool
import codebase.rag.GeminiConfig
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiVisionProvider429Test {

    private fun keyInstance(id: String) = LlmInstance(
        id = id,
        baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$id",
        model = "gemini-2.5-flash",
        quota = QuotaConfig(limitValue = 100, thresholdPercent = 80, resetPolicy = ResetPolicy.NEVER)
    )

    private class FakeGeminiVisionProvider(
        private val pool: GeminiKeyPool,
        private val failOnKey: String,
        private val successText: String = "== OCR OK"
    ) : GeminiVisionProvider(GeminiConfig()) {

        override suspend fun processImage(
            imageBytes: ByteArray,
            mimeType: String,
            language: String,
            model: String,
            maxTokens: Int
        ): String {
            val instance = pool.nextInstance()
            if (instance.id == failOnKey) {
                pool.markRateLimited(instance)
                throw IllegalStateException("HTTP 429 Too Many Requests — rate limited for key ${instance.id}")
            }
            return successText
        }
    }

    @Test
    fun `should rotate to next key when HTTP 429 received`() {
        val keyA = keyInstance("key-A")
        val keyB = keyInstance("key-B")
        val pool = GeminiKeyPool(listOf(keyA, keyB), rotationStrategy = RotationStrategy.ROUND_ROBIN)

        val provider = FakeGeminiVisionProvider(pool, failOnKey = "key-A", successText = "== OCR OK")

        val result = try {
            kotlinx.coroutines.runBlocking {
                provider.processImage(ByteArray(10), "image/png", "fr", "gemini-2.5-flash", 8192)
            }
        } catch (e: IllegalStateException) {
            kotlinx.coroutines.runBlocking {
                provider.processImage(ByteArray(10), "image/png", "fr", "gemini-2.5-flash", 8192)
            }
        }

        assertEquals("== OCR OK", result)
        assertTrue(pool.isRateLimited(keyA))
        assertFalse(pool.isRateLimited(keyB))
    }

    @Test
    fun `should skip rate-limited key on subsequent calls`() {
        val keyA = keyInstance("key-A")
        val keyB = keyInstance("key-B")
        val pool = GeminiKeyPool(listOf(keyA, keyB), rotationStrategy = RotationStrategy.ROUND_ROBIN)
        pool.markRateLimited(keyA)

        val provider = FakeGeminiVisionProvider(pool, failOnKey = "key-A", successText = "== OCR OK")

        val result = kotlinx.coroutines.runBlocking {
            provider.processImage(ByteArray(10), "image/png", "fr", "gemini-2.5-flash", 8192)
        }
        assertEquals("== OCR OK", result)
    }
}