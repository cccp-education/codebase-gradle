package codebase.ocr

import java.util.Locale

object OcrMetricsCalculator {

    private const val CHARS_PER_TOKEN = 4.0
    private const val IMAGE_BASE_TOKENS = 258

    private val geminiPricing = mapOf(
        "gemini-2.5-flash" to Pricing(inputPerMillion = 0.15, outputPerMillion = 0.60),
        "gemini-2.5-pro" to Pricing(inputPerMillion = 1.25, outputPerMillion = 5.00),
        "gemini-1.5-flash" to Pricing(inputPerMillion = 0.075, outputPerMillion = 0.30),
        "gemini-1.5-pro" to Pricing(inputPerMillion = 1.25, outputPerMillion = 5.00)
    )

    private data class Pricing(val inputPerMillion: Double, val outputPerMillion: Double)

    fun estimateInputTokens(fileSizeBytes: Long, isImage: Boolean): Int {
        if (isImage) {
            return IMAGE_BASE_TOKENS
        }
        return (fileSizeBytes / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
    }

    fun estimateOutputTokens(outputLengthChars: Int): Int {
        if (outputLengthChars == 0) return 0
        return (outputLengthChars / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
    }

    fun estimateCostUsd(model: String, inputTokens: Int, outputTokens: Int): Double {
        val pricing = geminiPricing[model] ?: return 0.0
        val inputCost = (inputTokens / 1_000_000.0) * pricing.inputPerMillion
        val outputCost = (outputTokens / 1_000_000.0) * pricing.outputPerMillion
        return inputCost + outputCost
    }

    fun buildMetrics(
        fileName: String,
        fileSizeBytes: Long,
        isImage: Boolean,
        provider: String,
        model: String,
        language: String,
        ocrDurationMs: Long,
        outputLengthChars: Int,
        anonymizationReplacements: Int,
        anonymizationCategories: List<String>
    ): OcrMetrics {
        val inputTokens = estimateInputTokens(fileSizeBytes, isImage)
        val outputTokens = estimateOutputTokens(outputLengthChars)
        val cost = estimateCostUsd(model, inputTokens, outputTokens)
        return OcrMetrics(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            isImage = isImage,
            provider = provider,
            model = model,
            language = language,
            ocrDurationMs = ocrDurationMs,
            outputLengthChars = outputLengthChars,
            anonymizationReplacements = anonymizationReplacements,
            anonymizationCategories = anonymizationCategories,
            estimatedInputTokens = inputTokens,
            estimatedOutputTokens = outputTokens,
            estimatedCostUsd = cost
        )
    }

    fun mergeIngestMetrics(
        ocrMetrics: OcrMetrics,
        chunkCount: Int,
        ingestDurationMs: Long,
        embeddingDurationMs: Long
    ): OcrMetrics {
        return ocrMetrics.copy(
            chunkCount = chunkCount,
            ingestDurationMs = ingestDurationMs,
            embeddingDurationMs = embeddingDurationMs
        )
    }

    fun formatDurationMs(ms: Long): String {
        if (ms == 0L) return "0ms"
        if (ms < 1000) return "${ms}ms"
        if (ms < 60_000) return "${"%.1f".format(Locale.US, ms / 1000.0)}s"
        val minutes = ms / 60_000
        val seconds = (ms % 60_000) / 1000
        return "${minutes}m ${seconds}s"
    }
}
