package codebase.i18n

import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FakeLlmTranslatorTest {

    @Test
    fun `default translation is deterministic stub`() {
        val translator = FakeLlmTranslator()

        val result = translator.translate(
            TranslationRequest("Bonjour", "fr", "en")
        )

        assertIs<TranslationResult.Success>(result)
        assertEquals("[en] Bonjour", result.translatedText)
        assertEquals(1, translator.requestsReceived.size)
    }

    @Test
    fun `enqueued result is returned FIFO`() {
        val translator = FakeLlmTranslator()
        translator.enqueueResult(TranslationResult.Success("Hello"))
        translator.enqueueResult(TranslationResult.Failure("quota"))

        val first = translator.translate(TranslationRequest("Bonjour", "fr", "en"))
        val second = translator.translate(TranslationRequest("Merci", "fr", "en"))

        assertIs<TranslationResult.Success>(first)
        assertEquals("Hello", first.translatedText)
        assertIs<TranslationResult.Failure>(second)
        assertEquals("quota", second.reason)
        assertEquals(2, translator.requestsReceived.size)
    }
}