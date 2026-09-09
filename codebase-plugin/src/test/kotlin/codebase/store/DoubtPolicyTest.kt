package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC CB-FPA-RAG US-1 — doubt derivation from corpus text markers.
 *
 * The codex chunk corpus (chunks.json) carries no confidence column —
 * the doubt is derived heuristically from the visible OCR markers
 * (`[ILLISIBLE]` pages documented in book-ocr-issues.json). Baby-step
 * TDD strict: this test file is written first (RED), then the policy
 * object (GREEN).
 */
class DoubtPolicyTest {

    private fun chunk(
        content: String = "clean content",
        sectionPath: String = "Chapter 1 > Section 1.2",
        overlapNext: String? = null
    ): DocumentChunk = DocumentChunk(
        id = "chk-0000000000000001",
        sourceDocument = "book",
        sectionPath = sectionPath,
        headingLevel = 2,
        content = content,
        overlapNext = overlapNext,
        license = "PROPRIETARY"
    )

    @Test
    fun `content carrying the ILLISIBLE marker is doubtful with zero confidence`() {
        val doubt = DoubtPolicy.derive(chunk(content = "page [ILLISIBLE] transcription"))
        assertTrue(doubt.doubtful, "ILLISIBLE content must be flagged doubtful")
        assertEquals(0.0, doubt.confidence)
    }

    @Test
    fun `clean chunk keeps full default confidence`() {
        val doubt = DoubtPolicy.derive(chunk())
        assertFalse(doubt.doubtful)
        assertEquals(DoubtMetadata.MAX_CONFIDENCE, doubt.confidence)
    }

    @Test
    fun `sectionPath carrying the marker is doubtful`() {
        val doubt = DoubtPolicy.derive(chunk(sectionPath = "Chapitre > Page [ILLISIBLE]"))
        assertTrue(doubt.doubtful)
        assertEquals(0.0, doubt.confidence)
    }

    @Test
    fun `overlapNext carrying the marker is doubtful`() {
        val doubt = DoubtPolicy.derive(chunk(overlapNext = "suite [ILLISIBLE]..."))
        assertTrue(doubt.doubtful)
        assertEquals(0.0, doubt.confidence)
    }

    @Test
    fun `mark wraps every chunk with its derived doubt`() {
        val chunks = listOf(
            chunk(content = "page [ILLISIBLE]"),
            chunk(),
            chunk(content = "another clean page")
        )
        val marked = DoubtPolicy.mark(chunks)
        assertEquals(chunks.size, marked.size)
        assertTrue(marked[0].doubt.doubtful)
        assertFalse(marked[1].doubt.doubtful)
        assertFalse(marked[2].doubt.doubtful)
        assertEquals(chunks[0], marked[0].chunk)
    }

    @Test
    fun `marker constant is stable for corpus scanning`() {
        assertEquals("[ILLISIBLE]", DoubtPolicy.ILLISIBLE_MARKER)
    }
}