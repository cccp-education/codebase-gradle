package codebase.koog.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkIncrementalReportTest {

    @Test
    fun `hasChanges returns true when chunksAdded is non empty`() {
        val report = ChunkIncrementalReport(
            chunksAdded = listOf("id-a"),
            chunksModified = emptyList(),
            chunksRemoved = emptyList(),
            chunksUnchanged = emptyList()
        )

        assertTrue(report.hasChanges())
    }

    @Test
    fun `hasChanges returns true when chunksModified is non empty`() {
        val report = ChunkIncrementalReport(
            chunksAdded = emptyList(),
            chunksModified = listOf("id-b"),
            chunksRemoved = emptyList(),
            chunksUnchanged = emptyList()
        )

        assertTrue(report.hasChanges())
    }

    @Test
    fun `hasChanges returns true when chunksRemoved is non empty`() {
        val report = ChunkIncrementalReport(
            chunksAdded = emptyList(),
            chunksModified = emptyList(),
            chunksRemoved = listOf("id-c"),
            chunksUnchanged = emptyList()
        )

        assertTrue(report.hasChanges())
    }

    @Test
    fun `hasChanges returns false when only chunksUnchanged`() {
        val report = ChunkIncrementalReport(
            chunksAdded = emptyList(),
            chunksModified = emptyList(),
            chunksRemoved = emptyList(),
            chunksUnchanged = listOf("id-d")
        )

        assertFalse(report.hasChanges())
    }

    @Test
    fun `hasChanges returns false when all empty`() {
        val report = ChunkIncrementalReport(
            chunksAdded = emptyList(),
            chunksModified = emptyList(),
            chunksRemoved = emptyList(),
            chunksUnchanged = emptyList()
        )

        assertFalse(report.hasChanges())
    }

    @Test
    fun `counts reflect list sizes`() {
        val report = ChunkIncrementalReport(
            chunksAdded = listOf("id-a", "id-b"),
            chunksModified = listOf("id-c"),
            chunksRemoved = listOf("id-d", "id-e", "id-f"),
            chunksUnchanged = listOf("id-g")
        )

        assertEquals(2, report.chunksAddedCount)
        assertEquals(1, report.chunksModifiedCount)
        assertEquals(3, report.chunksRemovedCount)
        assertEquals(1, report.chunksUnchangedCount)
    }

    @Test
    fun `from produces report from chunk diff`() {
        val diff = ChunkDiff(
            added = listOf("id-a", "id-b"),
            modified = listOf("id-c"),
            removed = listOf("id-d"),
            unchanged = listOf("id-e", "id-f")
        )

        val report = ChunkIncrementalReport.from(diff)

        assertEquals(diff.added, report.chunksAdded)
        assertEquals(diff.modified, report.chunksModified)
        assertEquals(diff.removed, report.chunksRemoved)
        assertEquals(diff.unchanged, report.chunksUnchanged)
    }
}