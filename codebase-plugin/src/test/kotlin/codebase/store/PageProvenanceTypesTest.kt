package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC CB-PAGE-PROVENANCE US-1 — additive `pages` fields (D3, D5).
 *
 * Both [DocumentChunk] (ingestion) and [RetrieveResult] (retrieval)
 * carry the page provenance as a defaulted list — backward compat total
 * (missing/empty → `emptyList()`), exactly like the doubt metadata.
 */
class PageProvenanceTypesTest {

    @Test
    fun `DocumentChunk pages default to empty list`() {
        val chunk = DocumentChunk(
            id = "chk-1",
            sourceDocument = "book",
            sectionPath = "Chapter 1",
            headingLevel = 1,
            content = "content"
        )
        assertTrue(chunk.pages.isEmpty(), "pages should default to empty list")
    }

    @Test
    fun `DocumentChunk carries page provenance`() {
        val chunk = DocumentChunk(
            id = "chk-1",
            sourceDocument = "book",
            sectionPath = "Chapter 1",
            headingLevel = 1,
            content = "content",
            pages = listOf(40, 41)
        )
        assertEquals(listOf(40, 41), chunk.pages)
    }

    @Test
    fun `RetrieveResult pages default to empty list`() {
        val result = RetrieveResult(
            chunkId = 1L,
            chunkIndex = 0,
            chunkText = "text",
            sectionPath = "Chapter 1",
            headingLevel = 1,
            sourceDocument = "book",
            similarity = 0.9
        )
        assertTrue(result.pages.isEmpty(), "pages should default to empty list")
    }

    @Test
    fun `RetrieveResult carries page provenance with doubt metadata`() {
        val result = RetrieveResult(
            chunkId = 2L,
            chunkIndex = 1,
            chunkText = "shaky chunk",
            sectionPath = "Chapter 2",
            headingLevel = 2,
            sourceDocument = "illisible",
            similarity = 0.5,
            confidence = 0.2,
            doubtful = true,
            pages = listOf(7)
        )
        assertTrue(result.doubtful)
        assertEquals(0.2, result.confidence)
        assertEquals(listOf(7), result.pages)
    }
}