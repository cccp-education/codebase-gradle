package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC OCR-QUALITY US-4 — doubt-aware exposure of retrieved chunks.
 *
 * `DoubtExposure` is the pure kernel of the augmented-context policy :
 * confident chunks pass verbatim, doubtful chunks are annotated (or
 * excluded) so the LLM never cites shaky OCR text unknowingly.
 */
class DoubtExposureTest {

    private fun result(
        id: Long,
        text: String,
        similarity: Double = 0.9,
        confidence: Double = 1.0,
        doubtful: Boolean = false
    ) = RetrieveResult(
        chunkId = id,
        chunkIndex = 0,
        chunkText = text,
        sectionPath = "Chapter 1",
        headingLevel = 1,
        sourceDocument = "doc",
        similarity = similarity,
        confidence = confidence,
        doubtful = doubtful
    )

    @Test
    fun `confident chunks pass verbatim with no doubt marker`() {
        val results = listOf(result(1, "clean text"))
        val lines = DoubtExposure.expose(results)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("clean text"))
        assertFalse(lines[0].contains(DoubtExposure.DOUBT_MARKER))
        assertFalse(lines[0].lowercase().contains("confidence"))
    }

    @Test
    fun `doubtful chunks are annotated with marker and confidence`() {
        val results = listOf(result(1, "shaky text", confidence = 0.2, doubtful = true))
        val lines = DoubtExposure.expose(results)
        assertTrue(lines[0].contains(DoubtExposure.DOUBT_MARKER))
        assertTrue(lines[0].contains("0.20"))
        assertTrue(lines[0].contains("shaky text"))
    }

    @Test
    fun `doubtful chunks can be excluded entirely`() {
        val results = listOf(
            result(1, "clean text"),
            result(2, "shaky text", confidence = 0.2, doubtful = true)
        )
        val lines = DoubtExposure.expose(results, excludeDoubtful = true)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("clean text"))
    }

    @Test
    fun `exclusion keeps confident chunks in similarity order`() {
        val results = listOf(
            result(1, "shaky", confidence = 0.1, doubtful = true, similarity = 0.99),
            result(2, "second", similarity = 0.8),
            result(3, "third", similarity = 0.6)
        )
        val lines = DoubtExposure.expose(results, excludeDoubtful = true)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("second"))
        assertTrue(lines[1].contains("third"))
    }

    @Test
    fun `empty input produces empty output`() {
        assertTrue(DoubtExposure.expose(emptyList()).isEmpty())
    }

    @Test
    fun `all doubtful excluded produces empty output`() {
        val results = listOf(result(1, "shaky", confidence = 0.2, doubtful = true))
        assertTrue(DoubtExposure.expose(results, excludeDoubtful = true).isEmpty())
    }

    @Test
    fun `default policy annotates but keeps doubtful chunks`() {
        val results = listOf(result(1, "shaky text", confidence = 0.3, doubtful = true))
        val lines = DoubtExposure.expose(results)
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains(DoubtExposure.DOUBT_MARKER))
    }

    @Test
    fun `expose marker constant is stable`() {
        assertEquals(DoubtExposure.DOUBT_MARKER, DoubtExposure.DOUBT_MARKER)
        assertTrue(DoubtExposure.DOUBT_MARKER.isNotBlank())
    }
}