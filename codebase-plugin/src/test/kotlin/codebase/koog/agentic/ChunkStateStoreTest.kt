package codebase.koog.agentic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChunkStateStoreTest {

    @Test
    fun `load returns null when state file does not exist`(@TempDir tempDir: File) {
        val store = JsonChunkStateStore(File(tempDir, "chunk-state.json"))

        assertNull(store.load())
    }

    @Test
    fun `save and load round trip preserves chunk snapshot entries`(@TempDir tempDir: File) {
        val store = JsonChunkStateStore(File(tempDir, "chunk-state.json"))
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a"),
            ChunkSnapshotEntry("id-b", "BACKLOG.adoc", "1-3", "checksum-b")
        ))

        store.save(snapshot)
        val loaded = store.load()

        assertEquals(snapshot.entries.sortedBy { it.id }, loaded?.entries?.sortedBy { it.id })
    }

    @Test
    fun `save creates the state file`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "chunk-state.json")
        val store = JsonChunkStateStore(stateFile)
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a")
        ))

        store.save(snapshot)

        assertTrue(stateFile.exists(), "State file should be created")
        val content = stateFile.readText()
        assertTrue(content.contains("id-a"), "State file should contain chunk id")
        assertTrue(content.contains("AGENT.adoc"), "State file should contain source file")
        assertTrue(content.contains("5-10"), "State file should contain source lines")
        assertTrue(content.contains("checksum-a"), "State file should contain checksum")
    }

    @Test
    fun `load returns null when state file is empty`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "chunk-state.json").apply { writeText("") }
        val store = JsonChunkStateStore(stateFile)

        assertNull(store.load())
    }

    @Test
    fun `load returns null when state file is invalid json`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "chunk-state.json").apply { writeText("{not json") }
        val store = JsonChunkStateStore(stateFile)

        assertNull(store.load())
    }

    @Test
    fun `clear removes the state file if it exists`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "chunk-state.json")
        val store = JsonChunkStateStore(stateFile)
        store.save(ChunkSnapshot(listOf(ChunkSnapshotEntry("id-a", "AGENT.adoc", "5-10", "checksum-a"))))
        assertTrue(stateFile.exists())

        store.clear()

        assertFalse(stateFile.exists(), "State file should be removed after clear")
    }

    @Test
    fun `clear does not fail when state file does not exist`(@TempDir tempDir: File) {
        val store = JsonChunkStateStore(File(tempDir, "chunk-state.json"))

        store.clear()
    }

    @Test
    fun `round trip preserves source lines with dash and colon`(@TempDir tempDir: File) {
        val store = JsonChunkStateStore(File(tempDir, "chunk-state.json"))
        val snapshot = ChunkSnapshot(listOf(
            ChunkSnapshotEntry("id-x", "path/with/dash/file.adoc", "42-48", "checksum-x")
        ))

        store.save(snapshot)
        val loaded = store.load()

        assertEquals(snapshot.entries, loaded?.entries)
    }
}