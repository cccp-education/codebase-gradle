package codebase.koog.llm.adapter

import codebase.koog.llm.VisionProvider
import codex.ocr.OcrEngine
import codex.ocr.OcrRequest
import codex.ocr.OcrResult
import kotlinx.coroutines.runBlocking
import java.time.Instant

/**
 * Adapter that implements the codex [OcrEngine] port by delegating to a
 * codebase [VisionProvider] (AI-assisted OCR).
 *
 * Boundary rule (EPIC CDX-OCR-BOUNDARY): software OCR (Tesseract) is
 * actioned by codex; AI-assisted OCR is actioned by the codebase socle.
 * This adapter lives on the codebase side so the composition root can
 * inject AI vision into a codex `OcrPipeline`/`CollectOcrTask` without
 * creating an N2→N1 cycle: codebase already depends on the codex artifact.
 *
 * @param provider the AI vision provider backing this engine
 * @param model vision model identifier forwarded to the provider
 * @param maxTokens maximum tokens forwarded to the provider
 */
class VisionOcrEngineAdapter(
    private val provider: VisionProvider,
    private val model: String = "gemini-2.5-flash",
    private val maxTokens: Int = 8192
) : OcrEngine {

    override fun process(request: OcrRequest): OcrResult {
        val text = runBlocking {
            provider.processImage(
                imageBytes = request.imageData,
                mimeType = request.format,
                language = request.language,
                model = model,
                maxTokens = maxTokens
            )
        }
        return OcrResult(
            structuredText = text,
            confidence = 1.0,
            language = request.language,
            sourceFormat = request.format,
            generatedAt = Instant.now().toString(),
            model = model
        )
    }
}
