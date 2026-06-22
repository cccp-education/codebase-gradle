package codebase.i18n

import codebase.koog.llm.FakeLlmProvider
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LlmTranslatorTest {

    @Test
    fun `translate success returns sanitized LLM response`() {
        val llm = FakeLlmProvider().apply { nextResponse = "  \"Hello world\"  " }
        val translator = LlmTranslator(llm)

        val result = translator.translate(
            TranslationRequest("Bonjour le monde", "fr", "en")
        )

        assertIs<TranslationResult.Success>(result)
        assertEquals("Hello world", result.translatedText)
    }

    @Test
    fun `translate blank LLM response yields Failure`() {
        val llm = FakeLlmProvider().apply { nextResponse = "   " }
        val translator = LlmTranslator(llm)

        val result = translator.translate(
            TranslationRequest("Bonjour", "fr", "en")
        )

        assertIs<TranslationResult.Failure>(result)
        assertTrue(result.reason.contains("blank"))
    }

    @Test
    fun `translate LLM exception yields Failure`() {
        val llm = FakeLlmProvider().apply {
            nextResponse = ""
        }
        val translator = LlmTranslator(llm)

        val result = translator.translate(
            TranslationRequest("Bonjour", "fr", "en")
        )

        assertIs<TranslationResult.Failure>(result)
    }

    @Test
    fun `translate prompt contains source and target languages`() {
        val llm = FakeLlmProvider()
        val translator = LlmTranslator(llm)

        translator.translate(TranslationRequest("Hola mundo", "es", "fr"))

        assertEquals(1, llm.promptsReceived.size)
        val prompt = llm.promptsReceived.first()
        assertTrue(prompt.contains("es"), "prompt should mention source language")
        assertTrue(prompt.contains("fr"), "prompt should mention target language")
        assertTrue(prompt.contains("Hola mundo"), "prompt should contain source text")
    }

    @Test
    fun `translate same source and target language is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            TranslationRequest("Hello", "en", "en")
        }
    }

    @Test
    fun `translate blank source text is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            TranslationRequest("  ", "fr", "en")
        }
    }
}