package codebase.koog.llm

import codex.ocr.OcrEngine
import codex.ocr.OcrRequest
import org.slf4j.LoggerFactory

/**
 * Adapter qui wrap un [OcrEngine] de codex (Brooklyn) dans un [VisionProvider]
 * de codebase (Queens) pour l'intégrer dans la chaîne de fallback OCR.
 *
 * Boundary respecté : codex expose des engines OCR purs (sans IA),
 * codebase les adapte dans sa chaîne de fallback LLM.
 *
 * @param engine moteur OCR codex (ex: [codex.ocr.TesseractOcrEngine])
 */
class CodexOcrEngineAdapter(
    private val engine: OcrEngine
) : VisionProvider {

    private val log = LoggerFactory.getLogger(CodexOcrEngineAdapter::class.java)

    override suspend fun processImage(
        imageBytes: ByteArray,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        log.info(
            "[CodexOcrAdapter] Delegating to {} — mimeType={}, language={}, size={}bytes",
            engine::class.simpleName, mimeType, language, imageBytes.size
        )
        val request = OcrRequest(
            imageData = imageBytes,
            format = mimeType,
            language = language
        )
        val result = engine.process(request)
        return result.structuredText
    }
}