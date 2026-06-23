package codebase.koog.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkDiffTest {

    @Test
    fun `hasChanges returns true when added is non empty`() {
        val diff = ChunkDiff(added = listOf("id-a"), modified = emptyList(), removed = emptyList(), unchanged = emptyList())

        assertTrue(diff.hasChanges())
    }

    @Test
    fun `hasChanges returns true when modified is non empty`() {
        val diff = ChunkDiff(added = emptyList(), modified = listOf("id-b"), removed = emptyList(), unchanged = emptyList())

        assertTrue(diff.hasChanges())
    }

    @Test
    fun `hasChanges returns true when removed is non empty`() {
        val diff = ChunkDiff(added = emptyList(), modified = emptyList(), removed = listOf("id-c"), unchanged = emptyList())

        assertTrue(diff.hasChanges())
    }

    @Test
    fun `hasChanges returns false when only unchanged`() {
        val diff = ChunkDiff(added = emptyList(), modified = emptyList(), removed = emptyList(), unchanged = listOf("id-d"))

        assertFalse(diff.hasChanges())
    }

    @Test
    fun `hasChanges returns false when all empty`() {
        val diff = ChunkDiff(added = emptyList(), modified = emptyList(), removed = emptyList(), unchanged = emptyList())

        assertFalse(diff.hasChanges())
    }

    @Test
    fun `chunksToIngest returns added plus modified ids`() {
        val diff = ChunkDiff(
            added = listOf("id-a", "id-b"),
            modified = listOf("id-c"),
            removed = listOf("id-d"),
            unchanged = listOf("id-e")
        )

        assertEquals(listOf("id-a", "id-b", "id-c"), diff.chunksToIngest())
    }

    @Test
    fun `chunksToIngest returns empty when no added and no modified`() {
        val diff = ChunkDiff(added = emptyList(), modified = emptyList(), removed = emptyList(), unchanged = listOf("id-e"))

        assertTrue(diff.chunksToIngest().isEmpty())
    }
}