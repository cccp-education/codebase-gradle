package codebase.koog.agentic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GovernanceStateStoreTest {

    @Test
    fun `load returns null when state file does not exist`(@TempDir tempDir: File) {
        val store = JsonGovernanceStateStore(File(tempDir, "governance-state.json"))

        assertNull(store.load())
    }

    @Test
    fun `save and load round trip preserves snapshot entries`(@TempDir tempDir: File) {
        val store = JsonGovernanceStateStore(File(tempDir, "governance-state.json"))
        val snapshot = GovernanceFileSnapshot(listOf(
            ScannedFileEntry("AGENT.adoc", "abc123"),
            ScannedFileEntry(".agents/INDEX.adoc", "def456")
        ))

        store.save(snapshot)
        val loaded = store.load()

        assertEquals(snapshot.entries.sortedBy { it.relativePath }, loaded?.entries?.sortedBy { it.relativePath })
    }

    @Test
    fun `save creates the state file`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "governance-state.json")
        val store = JsonGovernanceStateStore(stateFile)
        val snapshot = GovernanceFileSnapshot(listOf(
            ScannedFileEntry("AGENT.adoc", "abc123")
        ))

        store.save(snapshot)

        assertTrue(stateFile.exists(), "State file should be created")
        val content = stateFile.readText()
        assertTrue(content.contains("AGENT.adoc"), "State file should contain file path")
        assertTrue(content.contains("abc123"), "State file should contain checksum")
    }

    @Test
    fun `load returns null when state file is empty`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "governance-state.json").apply { writeText("") }
        val store = JsonGovernanceStateStore(stateFile)

        assertNull(store.load())
    }

    @Test
    fun `load returns null when state file is invalid json`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "governance-state.json").apply { writeText("{not json") }
        val store = JsonGovernanceStateStore(stateFile)

        assertNull(store.load())
    }

    @Test
    fun `clear removes the state file if it exists`(@TempDir tempDir: File) {
        val stateFile = File(tempDir, "governance-state.json")
        val store = JsonGovernanceStateStore(stateFile)
        store.save(GovernanceFileSnapshot(listOf(ScannedFileEntry("AGENT.adoc", "abc123"))))
        assertTrue(stateFile.exists())

        store.clear()

        assertFalse(stateFile.exists(), "State file should be removed after clear")
    }

    @Test
    fun `clear does not fail when state file does not exist`(@TempDir tempDir: File) {
        val store = JsonGovernanceStateStore(File(tempDir, "governance-state.json"))

        store.clear()
    }
}