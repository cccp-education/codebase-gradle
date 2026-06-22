package contracts.i18n

/**
 * Contrat N0 — Service de traduction transverse (codebase-gradle N1).
 *
 * Consommé par bakery-gradle (US-I18N-MIG-4), document-gradle, slider-gradle.
 * Implémentations :
 *  - [codebase.i18n.LlmTranslator]     : production, wrap [codebase.koog.llm.LlmProvider]
 *  - [codebase.i18n.FakeLlmTranslator] : tests, déterministe sans réseau/clé API
 *
 * Baby-step : une chaîne source → une chaîne traduite. Pas de batch, pas de
 * glossaire, pas de contexte. Le contrat grossira si un borough en a besoin.
 */
interface TranslationService {

    fun translate(request: TranslationRequest): TranslationResult
}

data class TranslationRequest(
    val sourceText: String,
    val sourceLanguage: String,
    val targetLanguage: String
) {
    init {
        require(sourceText.isNotBlank()) { "sourceText must not be blank" }
        require(sourceLanguage.isNotBlank()) { "sourceLanguage must not be blank" }
        require(targetLanguage.isNotBlank()) { "targetLanguage must not be blank" }
        require(sourceLanguage != targetLanguage) {
            "sourceLanguage ($sourceLanguage) must differ from targetLanguage ($targetLanguage)"
        }
    }
}

sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult() {
        init { require(translatedText.isNotBlank()) { "translatedText must not be blank" } }
    }

    /** LLM indisponible, quota dépassé, réseau coupé, etc. */
    data class Failure(val reason: String) : TranslationResult()
}