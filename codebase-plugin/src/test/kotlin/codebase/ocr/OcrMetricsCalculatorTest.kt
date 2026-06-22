package codebase.ocr

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrMetricsCalculatorTest {

    @Test
    fun `estimateInputTokens from image bytes`() {
        val tokens = OcrMetricsCalculator.estimateInputTokens(1024 * 100, isImage = true)
        assertTrue(tokens > 0)
        assertTrue(tokens < 500)
    }

    @Test
    fun `estimateInputTokens from text bytes`() {
        val tokens = OcrMetricsCalculator.estimateInputTokens(1024 * 10, isImage = false)
        assertTrue(tokens > 0)
        assertTrue(tokens < 5000)
    }

    @Test
    fun `estimateOutputTokens from output length`() {
        val tokens = OcrMetricsCalculator.estimateOutputTokens(2000)
        assertTrue(tokens > 0)
        assertTrue(tokens < 1000)
    }

    @Test
    fun `estimateOutputTokens zero length returns zero`() {
        assertEquals(0, OcrMetricsCalculator.estimateOutputTokens(0))
    }

    @Test
    fun `estimateCostUsd for gemini-2-5-flash`() {
        val cost = OcrMetricsCalculator.estimateCostUsd(
            model = "gemini-2.5-flash",
            inputTokens = 1000,
            outputTokens = 500
        )
        assertTrue(cost > 0.0)
        assertTrue(cost < 0.01)
    }

    @Test
    fun `estimateCostUsd for gemini-2-5-pro`() {
        val cost = OcrMetricsCalculator.estimateCostUsd(
            model = "gemini-2.5-pro",
            inputTokens = 1000,
            outputTokens = 500
        )
        assertTrue(cost > 0.0)
        assertTrue(cost < 0.01)
    }

    @Test
    fun `estimateCostUsd for ollama returns zero`() {
        val cost = OcrMetricsCalculator.estimateCostUsd(
            model = "gpt-oss:120b-cloud",
            inputTokens = 1000,
            outputTokens = 500
        )
        assertEquals(0.0, cost)
    }

    @Test
    fun `estimateCostUsd for unknown model returns zero`() {
        val cost = OcrMetricsCalculator.estimateCostUsd(
            model = "unknown-model",
            inputTokens = 1000,
            outputTokens = 500
        )
        assertEquals(0.0, cost)
    }

    @Test
    fun `buildMetrics aggregates all fields`() {
        val metrics = OcrMetricsCalculator.buildMetrics(
            fileName = "scan.png",
            fileSizeBytes = 1024 * 200,
            isImage = true,
            provider = "gemini",
            model = "gemini-2.5-flash",
            language = "fr",
            ocrDurationMs = 1500,
            outputLengthChars = 3000,
            anonymizationReplacements = 3,
            anonymizationCategories = listOf("email", "phone", "apikey")
        )
        assertEquals("scan.png", metrics.fileName)
        assertEquals(1024 * 200, metrics.fileSizeBytes)
        assertEquals(true, metrics.isImage)
        assertEquals("gemini", metrics.provider)
        assertEquals("gemini-2.5-flash", metrics.model)
        assertEquals("fr", metrics.language)
        assertEquals(1500, metrics.ocrDurationMs)
        assertEquals(3000, metrics.outputLengthChars)
        assertEquals(3, metrics.anonymizationReplacements)
        assertEquals(listOf("email", "phone", "apikey"), metrics.anonymizationCategories)
        assertTrue(metrics.estimatedInputTokens > 0)
        assertTrue(metrics.estimatedOutputTokens > 0)
        assertTrue(metrics.estimatedCostUsd > 0.0)
    }

    @Test
    fun `buildMetrics with zero anonymization`() {
        val metrics = OcrMetricsCalculator.buildMetrics(
            fileName = "doc.txt",
            fileSizeBytes = 500,
            isImage = false,
            provider = "gemini",
            model = "gemini-2.5-flash",
            language = "en",
            ocrDurationMs = 200,
            outputLengthChars = 100,
            anonymizationReplacements = 0,
            anonymizationCategories = emptyList()
        )
        assertEquals(0, metrics.anonymizationReplacements)
        assertEquals(emptyList<String>(), metrics.anonymizationCategories)
    }

    @Test
    fun `buildMetrics with ollama provider has zero cost`() {
        val metrics = OcrMetricsCalculator.buildMetrics(
            fileName = "scan.png",
            fileSizeBytes = 1024 * 100,
            isImage = true,
            provider = "ollama",
            model = "gpt-oss:120b-cloud",
            language = "fr",
            ocrDurationMs = 3000,
            outputLengthChars = 2000,
            anonymizationReplacements = 0,
            anonymizationCategories = emptyList()
        )
        assertEquals(0.0, metrics.estimatedCostUsd)
    }

    @Test
    fun `mergeIngestMetrics adds chunk and duration fields`() {
        val ocrMetrics = OcrMetricsCalculator.buildMetrics(
            fileName = "scan.png",
            fileSizeBytes = 1024 * 100,
            isImage = true,
            provider = "gemini",
            model = "gemini-2.5-flash",
            language = "fr",
            ocrDurationMs = 1500,
            outputLengthChars = 3000,
            anonymizationReplacements = 0,
            anonymizationCategories = emptyList()
        )
        val merged = OcrMetricsCalculator.mergeIngestMetrics(
            ocrMetrics,
            chunkCount = 5,
            ingestDurationMs = 800,
            embeddingDurationMs = 2000
        )
        assertEquals(5, merged.chunkCount)
        assertEquals(800, merged.ingestDurationMs)
        assertEquals(2000, merged.embeddingDurationMs)
        assertEquals(1500, merged.ocrDurationMs)
    }

    @Test
    fun `formatDurationMs formats milliseconds to human readable`() {
        assertEquals("0ms", OcrMetricsCalculator.formatDurationMs(0))
        assertEquals("500ms", OcrMetricsCalculator.formatDurationMs(500))
        assertEquals("1.5s", OcrMetricsCalculator.formatDurationMs(1500))
        assertEquals("2m 3s", OcrMetricsCalculator.formatDurationMs(123000))
    }
}
