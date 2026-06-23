package codebase.koog.agentic

import contracts.vibecoding.registry.ToolRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GovernanceEnforcementWirerTest {

    @Test
    fun `wire should return same registry when no governance files`(@TempDir tempDir: File) {
        val registry = ToolRegistry()
        val wired = GovernanceEnforcementWirer.wire(registry, tempDir)

        assertEquals(registry, wired, "Should return same registry when no governance")
    }

    @Test
    fun `wire should return new registry with hook when INTERDIRE rule exists`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            * NE DOIT JAMAIS git push sans permission.
            """.trimIndent()
        )

        val registry = ToolRegistry()
        val wired = GovernanceEnforcementWirer.wire(registry, tempDir)

        assertTrue(wired !== registry, "Should return new registry with hook")
        val exception = org.junit.jupiter.api.assertThrows<SecurityException> {
            wired.execute("exec_shell", mapOf("command" to "git push origin main"), tempDir.absolutePath)
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED"))
        assertTrue(exception.message!!.contains("git push", ignoreCase = true))
    }

    @Test
    fun `wired registry should preserve registered tool metadata`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("* NE DOIT JAMAIS git push sans permission.\n")

        val registry = ToolRegistry()
        val wired = GovernanceEnforcementWirer.wire(registry, tempDir)

        assertTrue(wired.toolNames().contains("read_file"))
        assertTrue(wired.toolNames().contains("exec_shell"))
    }

    @Test
    fun `wired registry should allow safe commands`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("* NE DOIT JAMAIS git push sans permission.\n")

        val registry = ToolRegistry()
        val wired = GovernanceEnforcementWirer.wire(registry, tempDir)
        wired.registerHandler("exec_shell") { _, _, _ -> "safe_ok" }

        val result = wired.execute("exec_shell", mapOf("command" to "ls -la"), tempDir.absolutePath)
        assertEquals("safe_ok", result)
    }

    @Test
    fun `wired registry should block exec_gradle publish`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("* NE DOIT JAMAIS ./gradlew publish sans verification.\n")

        val registry = ToolRegistry()
        val wired = GovernanceEnforcementWirer.wire(registry, tempDir)

        val exception = org.junit.jupiter.api.assertThrows<SecurityException> {
            wired.execute("exec_gradle", mapOf("task" to "publish"), tempDir.absolutePath)
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [exec_gradle]"))
    }
}
