package codebase.scenarios

import codebase.i18n.FakeLlmTranslator
import codebase.i18n.LlmTranslator
import codebase.koog.llm.FakeLlmProvider
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TranslationWorld {
    var translator: TranslationService? = null
    var fakeLlmProvider: FakeLlmProvider? = null
    var result: TranslationResult? = null
    var caughtException: Throwable? = null
    var lastRequest: TranslationRequest? = null

    fun reset() {
        translator = null
        fakeLlmProvider = null
        result = null
        caughtException = null
        lastRequest = null
    }

    fun executeTranslation(sourceText: String, src: String, tgt: String) {
        val req = TranslationRequest(sourceText, src, tgt)
        lastRequest = req
        try {
            result = translator!!.translate(req)
        } catch (e: Throwable) {
            caughtException = e
        }
    }

    fun attemptTranslation(sourceText: String, src: String, tgt: String) {
        try {
            val req = TranslationRequest(sourceText, src, tgt)
            translator!!.translate(req)
        } catch (e: Throwable) {
            caughtException = e
        }
    }

    fun successResult(): TranslationResult.Success {
        val r = result
        requireNotNull(r) { "translation result is null" }
        assertIs<TranslationResult.Success>(r)
        return r
    }

    fun failureResult(): TranslationResult.Failure {
        val r = result
        requireNotNull(r) { "translation result is null" }
        assertIs<TranslationResult.Failure>(r)
        return r
    }

    fun fakeTranslator(): FakeLlmTranslator {
        val t = translator
        requireNotNull(t) { "translator not configured" }
        assertIs<FakeLlmTranslator>(t)
        return t
    }
}