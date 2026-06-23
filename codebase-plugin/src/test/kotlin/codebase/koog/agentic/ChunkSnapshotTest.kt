package codebase.koog.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunkSnapshotTest {

    @Test
    fun `empty snapshot has no entries and no ids`() {
        val snapshot = ChunkSnapshot.empty()

        assertTrue(snapshot.entries.isEmpty())
        assertTrue(snapshot.ids().isEmpty())
    }

    @Test
    fun `ids returns all chunk ids from entries`() {
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a"),
            ChunkSnapshotEntry("id-b", "BACKLOG.adoc", "1-3", "checksum-b")
        ))

        assertEquals(setOf("id-a", "id-b"), snapshot.ids())
    }

    @Test
    fun `entryOf returns entry for known chunk id`() {
        val entry = ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a")
        val snapshot = ChunkSnapshot(listOf(entry))

        assertEquals(entry, snapshot.entryOf("id-a"))
    }

    @Test
    fun `entryOf returns null for unknown chunk id`() {
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a")
        ))

        assertNull(snapshot.entryOf("id-unknown"))
    }

    @Test
    fun `checksumOf returns checksum for known chunk id`() {
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a")
        ))

        assertEquals("checksum-a", snapshot.checksumOf("id-a"))
    }

    @Test
    fun `checksumOf returns null for unknown chunk id`() {
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a")
        ))

        assertNull(snapshot.checksumOf("id-unknown"))
    }

    @Test
    fun `fromChunks builds snapshot from agentic chunks using their checksum`() {
        val chunkA = agenticChunk("id-a", "AGENT.adoc", "5-10", "content-a")
        val chunkB = agenticChunk("id-b", "BACKLOG.adoc", "1-3", "content-b")
        val snapshot = ChunkSnapshot.fromChunks(listOf(chunkA, chunkB))

        assertEquals(2, snapshot.entries.size)
        assertEquals("id-a", snapshot.entries[0].id)
        assertEquals("AGENT.adoc", snapshot.entries[0].sourceFile)
        assertEquals("5-10", snapshot.entries[0].sourceLines)
        assertEquals(chunkA.checksum, snapshot.entries[0].checksum)
        assertEquals("id-b", snapshot.entries[1].id)
        assertEquals(chunkB.checksum, snapshot.entries[1].checksum)
    }

    @Test
    fun `fromChunks preserves insertion order`() {
        val chunkB = agenticChunk("id-b", "BACKLOG.adoc", "1-3", "content-b")
        val chunkA = agenticChunk("id-a", "AGENT.adoc", "5-10", "content-a")
        val snapshot = ChunkSnapshot.fromChunks(listOf(chunkB, chunkA))

        assertEquals(listOf("id-b", "id-a"), snapshot.entries.map { it.id })
    }

    @Test
    fun `fromChunks with empty list returns empty snapshot`() {
        val snapshot = ChunkSnapshot.fromChunks(emptyList())

        assertTrue(snapshot.entries.isEmpty())
    }

    private fun agenticChunk(id: String, sourceFile: String, sourceLines: String, content: String): AgenticChunk =
        AgenticChunk(
            id = id,
            sourceFile = sourceFile,
            sourceLines = sourceLines,
            chunkType = ChunkType.RULE,
            content = content,
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = sha256(content)
        )

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}