package codebase.ocr

data class OcrMetrics(
    val fileName: String,
    val fileSizeBytes: Long,
    val isImage: Boolean,
    val provider: String,
    val model: String,
    val language: String,
    val ocrDurationMs: Long,
    val outputLengthChars: Int,
    val anonymizationReplacements: Int,
    val anonymizationCategories: List<String>,
    val estimatedInputTokens: Int,
    val estimatedOutputTokens: Int,
    val estimatedCostUsd: Double,
    val chunkCount: Int = 0,
    val ingestDurationMs: Long = 0,
    val embeddingDurationMs: Long = 0
)
