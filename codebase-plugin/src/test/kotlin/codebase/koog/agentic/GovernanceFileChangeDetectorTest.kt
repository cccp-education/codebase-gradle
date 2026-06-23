package codebase.koog.agentic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GovernanceFileChangeDetectorTest {

    private val detector = GovernanceFileChangeDetector()

    @Test
    fun `diff returns all current files as added when previous snapshot is empty`() {
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned(".agents/INDEX.adoc", "= Index\n")
        ))

        val diff = detector.diff(GovernanceFileSnapshot.empty(), current)

        assertEquals(listOf("AGENT.adoc", ".agents/INDEX.adoc").sorted(), diff.added.sorted())
        assertTrue(diff.modified.isEmpty(), "No modified when previous is empty")
        assertTrue(diff.removed.isEmpty(), "No removed when previous is empty")
        assertTrue(diff.unchanged.isEmpty(), "No unchanged when previous is empty")
    }

    @Test
    fun `diff marks unchanged files when checksum matches`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog\n")
        ))
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog\n")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty(), "No added when identical")
        assertTrue(diff.modified.isEmpty(), "No modified when identical")
        assertTrue(diff.removed.isEmpty(), "No removed when identical")
        assertEquals(listOf("AGENT.adoc", "BACKLOG.adoc").sorted(), diff.unchanged.sorted())
    }

    @Test
    fun `diff detects modified files when checksum differs`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n")
        ))
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n\n* NE DOIT JAMAIS committer\n")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty(), "No added when only modified")
        assertEquals(listOf("AGENT.adoc"), diff.modified)
        assertTrue(diff.removed.isEmpty(), "No removed when only modified")
        assertTrue(diff.unchanged.isEmpty(), "No unchanged when only modified")
    }

    @Test
    fun `diff detects removed files present in previous but not in current`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog\n")
        ))
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n")
        ))

        val diff = detector.diff(previous, current)

        assertTrue(diff.added.isEmpty(), "No added when only removed")
        assertTrue(diff.modified.isEmpty(), "No modified when only removed")
        assertEquals(listOf("BACKLOG.adoc"), diff.removed)
        assertEquals(listOf("AGENT.adoc"), diff.unchanged)
    }

    @Test
    fun `diff detects mixed added modified removed and unchanged files`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog\n"),
            scanned("TODO.adoc", "= Todo\n")
        ))
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog updated\n"),
            scanned("INDEX.adoc", "= Index\n")
        ))

        val diff = detector.diff(previous, current)

        assertEquals(listOf("INDEX.adoc"), diff.added)
        assertEquals(listOf("BACKLOG.adoc"), diff.modified)
        assertEquals(listOf("TODO.adoc"), diff.removed)
        assertEquals(listOf("AGENT.adoc"), diff.unchanged)
    }

    @Test
    fun `diff with empty current returns all previous files as removed`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n")
        ))

        val diff = detector.diff(previous, GovernanceFileSnapshot.empty())

        assertEquals(listOf("AGENT.adoc"), diff.removed)
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `diff empty previous and empty current returns empty diff`() {
        val diff = detector.diff(GovernanceFileSnapshot.empty(), GovernanceFileSnapshot.empty())

        assertTrue(diff.added.isEmpty())
        assertTrue(diff.modified.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun `diff provides paths to ingest as added plus modified`() {
        val previous = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog\n")
        ))
        val current = GovernanceFileSnapshot(listOf(
            scanned("AGENT.adoc", "= Agent\n"),
            scanned("BACKLOG.adoc", "= Backlog updated\n"),
            scanned("INDEX.adoc", "= Index\n")
        ))

        val diff = detector.diff(previous, current)

        assertEquals(listOf("BACKLOG.adoc", "INDEX.adoc").sorted(), diff.pathsToIngest().sorted())
    }

    private fun scanned(path: String, content: String): ScannedFileEntry =
        ScannedFileEntry(path, sha256(content))

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}