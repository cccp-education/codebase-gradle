package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Characterization test — EPIC CDX-RAG-1 : mirror of codex `ChunkIndexAssignmentTest`.
 *
 * Verbatim migration of `codex.store.IngestIndexing` into `codebase.store`
 * (Brooklyn → Queens, N2 → N1). The logic is identical — only the package
 * changes. This test guards the behavior contract before RAG-2 rewires the
 * codebase call sites.
 */
class IngestIndexingTest {

    @Test
    fun `local indices are sequential per document`() {
        val chunks = listOf(
            chunk("doc-A", "Section 1", "Content 1"),
            chunk("doc-A", "Section 2", "Content 2"),
            chunk("doc-A", "Section 3", "Content 3")
        )
        val indices = IngestIndexing.assignLocalIndices(chunks)
        assertEquals(listOf(0, 1, 2), indices["doc-A"])
    }

    @Test
    fun `duplicate chunks in same document get distinct indices`() {
        val duplicateContent = "Identical body repeated twice."
        val chunks = listOf(
            chunk("doc-A", "Section 1", duplicateContent),
            chunk("doc-A", "Section 2", duplicateContent)
        )
        val indices = IngestIndexing.assignLocalIndices(chunks)
        assertEquals(listOf(0, 1), indices["doc-A"])
        assertNotEquals(
            indices["doc-A"]!![0],
            indices["doc-A"]!![1],
            "Duplicate chunks must not share the same chunk_index (CDX-CR3-3)"
        )
    }

    @Test
    fun `indices are local per document, not global`() {
        val chunks = listOf(
            chunk("doc-A", "Section 1", "Content A1"),
            chunk("doc-B", "Section 1", "Content B1"),
            chunk("doc-B", "Section 2", "Content B2")
        )
        val indices = IngestIndexing.assignLocalIndices(chunks)
        assertEquals(listOf(0), indices["doc-A"])
        assertEquals(listOf(0, 1), indices["doc-B"])
    }

    @Test
    fun `three duplicate chunks produce 0 1 2 not 0 0 0`() {
        val body = "Same body."
        val chunks = listOf(
            chunk("doc-A", "S1", body),
            chunk("doc-A", "S2", body),
            chunk("doc-A", "S3", body)
        )
        val indices = IngestIndexing.assignLocalIndices(chunks)
        assertEquals(listOf(0, 1, 2), indices["doc-A"])
        assertTrue(indices["doc-A"]!!.toSet().size == 3, "All indices must be unique")
    }

    @Test
    fun `empty chunks produce empty index map`() {
        val indices = IngestIndexing.assignLocalIndices(emptyList())
        assertTrue(indices.isEmpty())
    }

    @Test
    fun `single document single chunk produces index 0`() {
        val indices = IngestIndexing.assignLocalIndices(
            listOf(chunk("solo", "S1", "only one"))
        )
        assertEquals(listOf(0), indices["solo"])
    }

    private fun chunk(source: String, section: String, content: String): DocumentChunk =
        DocumentChunk(
            id = "chk-$source-$section",
            sourceDocument = source,
            sectionPath = section,
            headingLevel = 1,
            content = content,
            license = "Apache-2.0"
        )
}