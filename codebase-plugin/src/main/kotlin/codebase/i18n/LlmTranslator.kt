package codebase.i18n

import codebase.koog.llm.LlmProvider
import contracts.i18n.TranslationRequest
import contracts.i18n.TranslationResult
import contracts.i18n.TranslationService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Implémentation N1 de [TranslationService] — wrap [LlmProvider].
 *
 * En production : GeminiLlmProvider ou OllamaChatModel.
 * En test : FakeLlmProvider (injecté via ce même constructeur).
 *
 * Le prompt est minimal : "Translate from <src> to <tgt>:\n<text>".
 * Pas de glossaire, pas de few-shot — baby-step.
 */
class LlmTranslator(private val llm: LlmProvider) : TranslationService {

    private val log = LoggerFactory.getLogger(LlmTranslator::class.java)

    override fun translate(request: TranslationRequest): TranslationResult {
        val prompt = buildPrompt(request)
        log.info(
            "[LlmTranslator] translate {}→{} ({} chars)",
            request.sourceLanguage,
            request.targetLanguage,
            request.sourceText.length
        )
        return try {
            val raw = runBlocking { llm.call(prompt) }
            val cleaned = sanitize(raw)
            if (cleaned.isBlank()) {
                TranslationResult.Failure("LLM returned blank response")
            } else {
                TranslationResult.Success(cleaned)
            }
        } catch (e: Exception) {
            log.warn("[LlmTranslator] LLM call failed: {}", e.message)
            TranslationResult.Failure(e.message ?: "LLM call failed")
        }
    }

    private fun buildPrompt(request: TranslationRequest): String =
        """Translate from ${request.sourceLanguage} to ${request.targetLanguage}.
Preserve ALL backtick code spans (`...`) exactly as-is — never modify backtick content, spacing, or position.
This text may be a fragment of a larger sentence — translate the fragment without requesting more context.
Output ONLY the translated text with zero explanation or commentary.

${request.sourceText}"""

    /** Retire les quotes/guillemets qu'un LLM ajoute souvent autour de la traduction. */
    private fun sanitize(raw: String): String = raw.trim().trim('"', '«', '»', '`', '\n')
}