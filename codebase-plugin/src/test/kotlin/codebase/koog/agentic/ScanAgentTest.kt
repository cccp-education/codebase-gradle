package codebase.koog.agentic

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanAgentTest {

    private val scanAgent = ScanAgent()

    @Test
    fun `scan returns empty list for empty directory`(@TempDir tempDir: File) {
        val result = scanAgent.scan(tempDir)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scan finds adoc files at root level`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(2, result.size)
        assertTrue(result.any { it.relativePath == "AGENT.adoc" })
        assertTrue(result.any { it.relativePath == "BACKLOG.adoc" })
    }

    @Test
    fun `scan recursively finds all adoc files in agents directory`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n")
        val agentsDir = File(tempDir, ".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Index\n")
        File(agentsDir, "SESSIONS_HISTORY.adoc").writeText("= History\n")
        val sessionsDir = File(agentsDir, "sessions").apply { mkdirs() }
        File(sessionsDir, "001-test.adoc").writeText("= Session 1\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(4, result.size)
        assertTrue(result.any { it.relativePath == "AGENT.adoc" })
        assertTrue(result.any { it.relativePath == ".agents/INDEX.adoc" })
        assertTrue(result.any { it.relativePath == ".agents/SESSIONS_HISTORY.adoc" })
        assertTrue(result.any { it.relativePath == ".agents/sessions/001-test.adoc" })
    }

    @Test
    fun `scan finds adoc files in subproject directories`(@TempDir tempDir: File) {
        val subproject = File(tempDir, "codebase-plugin").apply { mkdirs() }
        File(subproject, "AGENT.adoc").writeText("= Sub Agent\n")
        File(subproject, "BACKLOG.adoc").writeText("= Sub Backlog\n")
        val agentsDir = File(subproject, ".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Sub Index\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(3, result.size)
        assertTrue(result.any { it.relativePath.contains("codebase-plugin") })
        assertTrue(result.any { it.relativePath.contains("AGENT.adoc") })
        assertTrue(result.any { it.relativePath.contains("BACKLOG.adoc") })
        assertTrue(result.any { it.relativePath.contains("INDEX.adoc") })
    }

    @Test
    fun `scan ignores build and git directories`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n")
        val buildDir = File(tempDir, "build").apply { mkdirs() }
        File(buildDir, "generated.adoc").writeText("= Generated\n")
        val gitDir = File(tempDir, ".git").apply { mkdirs() }
        File(gitDir, "README.adoc").writeText("= Git\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(1, result.size)
        assertTrue(result.all { !it.relativePath.contains("build") })
        assertTrue(result.all { !it.relativePath.contains(".git") })
    }

    @Test
    fun `scan reads file content correctly`(@TempDir tempDir: File) {
        val content = "= Agent\n\n* NE DOIT JAMAIS committer\n"
        File(tempDir, "AGENT.adoc").writeText(content)

        val result = scanAgent.scan(tempDir)

        assertEquals(1, result.size)
        assertEquals(content, result[0].content)
    }

    @Test
    fun `scan ignores non-adoc files`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n")
        File(tempDir, "config.yml").writeText("key: value\n")
        File(tempDir, "README.md").writeText("# README\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(1, result.size)
        assertEquals("AGENT.adoc", result[0].relativePath)
    }

    @Test
    fun `scan of subproject with nested agents sessions finds all adoc recursively`(@TempDir tempDir: File) {
        val sub = File(tempDir, "my-plugin").apply { mkdirs() }
        File(sub, "AGENT.adoc").writeText("= Agent\n")
        val agentsDir = File(sub, ".agents").apply { mkdirs() }
        File(agentsDir, "INDEX.adoc").writeText("= Index\n")
        val archivesDir = File(agentsDir, "archives").apply { mkdirs() }
        File(archivesDir, "COMPLETED_TASKS.adoc").writeText("= Archive\n")

        val result = scanAgent.scan(tempDir)

        assertEquals(3, result.size)
        assertTrue(result.any { it.relativePath == "my-plugin/AGENT.adoc" })
        assertTrue(result.any { it.relativePath == "my-plugin/.agents/INDEX.adoc" })
        assertTrue(result.any { it.relativePath == "my-plugin/.agents/archives/COMPLETED_TASKS.adoc" })
    }
}