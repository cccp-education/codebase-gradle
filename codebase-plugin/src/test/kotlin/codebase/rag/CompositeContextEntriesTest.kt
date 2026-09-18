package codebase.rag

import codebase.store.RetrieveResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EPIC CB-PAGE-PROVENANCE US-3 (D8) — pure kernel of the N1 exposition.
 *
 * Both downstream surfaces that expose retrieved chunks carry `pages` as
 * an additive field, omitted when empty (backward compat total):
 *  - the codex JSON entries of `CodebaseCompositeContextTask`;
 *  - the Docs channel meta line of `CompositeContextBuilder`.
 */
class CompositeContextEntriesTest {

    private val result = RetrieveResult(
        chunkId = 7L,
        chunkIndex = 0,
        chunkText = "referentiel competences",
        sectionPath = "Chapter 3",
        headingLevel = 1,
        sourceDocument = "livre.adoc",
        similarity = 0.512,
        pages = listOf(40, 41)
    )

    @Test
    fun `codex entry exposes pages when present`() {
        val entry = CompositeContextEntries.codexEntry(result)
        assertEquals(listOf(40, 41), entry["pages"])
    }

    @Test
    fun `codex entry omits pages when empty`() {
        val entry = CompositeContextEntries.codexEntry(result.copy(pages = emptyList()))
        assertFalse(entry.containsKey("pages"), "pages should be omitted when empty")
    }

    @Test
    fun `codex entry keeps the legacy key set`() {
        val entry = CompositeContextEntries.codexEntry(result)
        assertEquals("codex", entry["source"])
        assertEquals("livre.adoc", entry["sourceDocument"])
        assertEquals("Chapter 3", entry["sectionPath"])
        assertEquals(1, entry["headingLevel"])
        assertEquals(7L, entry["chunkId"])
        assertEquals(0.512, entry["similarity"])
        assertTrue((entry["chunkText"] as String).startsWith("referentiel"))
    }

    @Test
    fun `docs meta line appends pages when present`() {
        val line = CompositeContextEntries.docsMetaLine(result)
        assertEquals("[Doc] source=livre.adoc section=Chapter 3 sim=0.512 pages=[40,41]", line)
    }

    @Test
    fun `docs meta line stays byte-identical when pages absent`() {
        val line = CompositeContextEntries.docsMetaLine(result.copy(pages = emptyList()))
        assertEquals("[Doc] source=livre.adoc section=Chapter 3 sim=0.512", line)
        assertFalse(line.contains("pages="), "no pages fragment when empty")
    }
}