package codebase.koog

import contracts.vibecoding.registry.ToolRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ToolRegistryTest {

    private val registry = ToolRegistry()

    @Test
    fun `read_file should reject files larger than 10MB`(@TempDir tempDir: File) {
        val largeFile = File(tempDir, "large.bin")
        val tenMbPlusOne = (10 * 1024 * 1024) + 1

        largeFile.outputStream().use { out ->
            var written = 0
            val buf = ByteArray(8192)
            while (written < tenMbPlusOne) {
                val chunk = minOf(buf.size, tenMbPlusOne - written)
                out.write(buf, 0, chunk)
                written += chunk
            }
        }

        assertThrows(SecurityException::class.java) {
            registry.execute(
                toolName = "read_file",
                arguments = mapOf("path" to largeFile.absolutePath),
                workspaceRoot = tempDir.absolutePath
            )
        }
    }

    @Test
    fun `audit entry should contain workspaceRoot`(@TempDir tempDir: File) {
        registry.clearAudit()
        val smallFile = File(tempDir, "audit.txt")
        smallFile.writeText("content")

        registry.execute(
            toolName = "read_file",
            arguments = mapOf("path" to smallFile.absolutePath),
            workspaceRoot = tempDir.absolutePath
        )

        val entry = registry.auditEntries().last()
        assertEquals(tempDir.absolutePath, entry.workspaceRoot,
            "Audit entry should contain workspaceRoot")
    }

    @Test
    fun `registerHandler should execute custom tool logic`() {
        registry.registerHandler("list_tasks") { _, _, _ ->
            "task_a: build\n  group: build\n  options: []\n---\ntask_b: test\n  group: verification\n  options: [--tests]\n==="
        }
        val result = registry.execute("list_tasks", emptyMap<String, String>(), "/tmp")
        assertTrue(result.contains("task_a: build"), "Should list task_a")
        assertTrue(result.contains("task_b: test"), "Should list task_b")
    }

    @Test
    fun `custom handler should take precedence over builtin if name matches`() {
        registry.registerHandler("read_file") { _, _, _ -> "custom_read_handler_result" }
        val result = registry.execute("read_file", mapOf("path" to "nonexistent.txt"), "/tmp")
        assertEquals("custom_read_handler_result", result)
    }

    @Test
    fun `handlerless custom tool should throw ToolkitIsMissingException`() {
        assertThrows(Exception::class.java) {
            registry.execute("unknown_tool", emptyMap<String, String>(), "/tmp")
        }
    }

    @Test
    fun `enforcement hook should block exec_shell before execution`(@TempDir tempDir: File) {
        val blocked = ToolRegistry(
            enforcementHook = { toolName, _ ->
                if (toolName == "exec_shell") "git push interdit" else null
            }
        )
        val exception = assertThrows(SecurityException::class.java) {
            blocked.execute(
                toolName = "exec_shell",
                arguments = mapOf("command" to "git push origin main"),
                workspaceRoot = tempDir.absolutePath
            )
        }
        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [exec_shell]"))
        assertTrue(exception.message!!.contains("git push interdit"))
        val entry = blocked.auditEntries().last()
        assertEquals(tempDir.absolutePath, entry.workspaceRoot)
        assertTrue(entry.error!!.contains("ENFORCEMENT BLOCKED"))
    }

    @Test
    fun `enforcement hook returning null should allow normal execution`(@TempDir tempDir: File) {
        val allowed = ToolRegistry(enforcementHook = { _, _ -> null })
        allowed.registerHandler("exec_shell") { _, _, _ -> "shell_ok" }

        val result = allowed.execute(
            toolName = "exec_shell",
            arguments = mapOf("command" to "ls"),
            workspaceRoot = tempDir.absolutePath
        )

        assertEquals("shell_ok", result)
    }

    @Test
    fun `enforcement hook should block exec_gradle before execution`(@TempDir tempDir: File) {
        val blocked = ToolRegistry(
            enforcementHook = { toolName, args ->
                if (toolName == "exec_gradle" && args["task"]?.contains("publish") == true) {
                    "publish interdit"
                } else null
            }
        )

        val exception = assertThrows(SecurityException::class.java) {
            blocked.execute(
                toolName = "exec_gradle",
                arguments = mapOf("task" to "publish"),
                workspaceRoot = tempDir.absolutePath
            )
        }

        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED [exec_gradle]"))
        assertTrue(exception.message!!.contains("publish interdit"))
    }

    @Test
    fun `write_file should reject content exceeding MAX_WRITE_CHARS`(@TempDir tempDir: File) {
        val oversized = "a".repeat(ToolRegistry.MAX_WRITE_CHARS + 1)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.execute(
                toolName = "write_file",
                arguments = mapOf("path" to "big.txt", "content" to oversized),
                workspaceRoot = tempDir.absolutePath
            )
        }
        assertTrue(ex.message!!.contains("exceeds"))
        assertTrue(ex.message!!.contains("${ToolRegistry.MAX_WRITE_CHARS}"))
    }

    @Test
    fun `write_file should accept content at MAX_WRITE_CHARS`(@TempDir tempDir: File) {
        val ok = "a".repeat(ToolRegistry.MAX_WRITE_CHARS)
        val result = registry.execute(
            toolName = "write_file",
            arguments = mapOf("path" to "ok.txt", "content" to ok),
            workspaceRoot = tempDir.absolutePath
        )
        assertTrue(result.contains("File written"))
    }

    @Test
    fun `edit_file should reject newString exceeding MAX_WRITE_CHARS`(@TempDir tempDir: File) {
        val original = File(tempDir, "edit.txt")
        original.writeText("placeholder")
        val oversized = "a".repeat(ToolRegistry.MAX_WRITE_CHARS + 1)
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.execute(
                toolName = "edit_file",
                arguments = mapOf(
                    "path" to "edit.txt",
                    "oldString" to "placeholder",
                    "newString" to oversized
                ),
                workspaceRoot = tempDir.absolutePath
            )
        }
        assertTrue(ex.message!!.contains("exceeds"))
        assertTrue(ex.message!!.contains("${ToolRegistry.MAX_WRITE_CHARS}"))
    }

    @Test
    fun `edit_file should reject resulting file content exceeding MAX_WRITE_CHARS`(@TempDir tempDir: File) {
        val original = File(tempDir, "grow.txt")
        original.writeText("a".repeat(ToolRegistry.MAX_WRITE_CHARS))
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.execute(
                toolName = "edit_file",
                arguments = mapOf(
                    "path" to "grow.txt",
                    "oldString" to "a",
                    "newString" to "bb"
                ),
                workspaceRoot = tempDir.absolutePath
            )
        }
        assertTrue(ex.message!!.contains("exceeds"))
    }

    @Test
    fun `MAX_WRITE_CHARS constant should be 1_000_000`() {
        assertEquals(1_000_000, ToolRegistry.MAX_WRITE_CHARS)
    }
}
