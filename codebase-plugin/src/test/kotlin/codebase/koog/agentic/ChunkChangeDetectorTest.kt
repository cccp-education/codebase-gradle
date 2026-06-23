package codebase.koog.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkChangeDetectorTest {

    private val detector = ChunkChangeDetector()

    @Test
    fun `diff returns all current chunks as added when previous snapshot is empty`() {
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b")
        ))

        val diff = detector.diff(ChunkSnapshot.empty(), current)

        assertEquals(listOf("id-a", "id-b").sorted(), diff.added.sorted())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `diff marks unchanged chunks when checksum matches`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b")
        ))
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertEquals(listOf("id-a", "id-b").sorted(), diff.unchanged.sorted())
    }

    @Test
    fun `diff detects modified chunks when checksum differs`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a-v1")
        ))
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a-v2")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty())
        assertEquals(listOf("id-a"), diff.modified)
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `diff detects removed chunks present in previous but not in current`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b")
        ))
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertEquals(listOf("id-b"), diff.removed)
        assertEquals(listOf("id-a"), diff.unchanged)
    }

    @Test
    fun `diff detects mixed added modified removed and unchanged chunks`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b-v1"),
            entry("id-c", "checksum-c")
        ))
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b-v2"),
            entry("id-d", "checksum-d")
        ))

        val diff = detector.diff(previous, current)

        assertEquals(listOf("id-d"), diff.added)
        assertEquals(listOf("id-b"), diff.modified)
        assertEquals(listOf("id-c"), diff.removed)
        assertEquals(listOf("id-a"), diff.unchanged)
    }

    @Test
    fun `diff with empty current returns all previous chunks as removed`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a")
        ))

        val diff = detector.diff(previous, ChunkSnapshot.empty())

        assertEquals(listOf("id-a"), diff.removed)
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `diff empty previous and empty current returns empty diff`() {
        val diff = detector.diff(ChunkSnapshot.empty(), ChunkSnapshot.empty())

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `chunksToIngest returns added plus modified ids`() {
        val previous = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b-v1")
        ))
        val current = ChunkSnapshot(listOf(
            entry("id-a", "checksum-a"),
            entry("id-b", "checksum-b-v2"),
            entry("id-c", "checksum-c")
        ))

        val diff = detector.diff(previous, current)

        assertEquals(listOf("id-b", "id-c").sorted(), diff.chunksToIngest().sorted())
    }

    private fun entry(id: String, checksum: String): ChunkSnapshotEntry =
        ChunkSnapshotEntry(id, "AGENT.adoc", "1-5", checksum)
}