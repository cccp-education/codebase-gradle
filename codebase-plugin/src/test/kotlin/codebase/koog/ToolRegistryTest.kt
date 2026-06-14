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
}
