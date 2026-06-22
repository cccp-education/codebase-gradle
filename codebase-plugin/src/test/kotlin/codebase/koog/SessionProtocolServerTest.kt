package codebase.koog

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.UUID

class SessionProtocolServerTest {

    @Test
    fun `single prompt produces single response`(@TempDir tempDir: Path) {
        val promptJson = """{"sessionId":"550e8400-e29b-41d4-a716-446655440000","prompt":"Add dark mode toggle","maxActions":3}"""

        val stdin = ByteArrayInputStream("$promptJson\n\n".toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        server.run(stdin, stdout)

        val output = stdout.toString(StandardCharsets.UTF_8).trim()
        val mapper = jacksonObjectMapper()
        val response: Map<String, Any> = mapper.readValue(output)

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response["sessionId"])
        assertTrue((response["output"] as String).contains("Add dark mode toggle"))
        assertTrue(listOf("COMPLETED", "IN_PROGRESS").contains(response["status"]))
    }

    @Test
    fun `multiple prompts produce multiple responses`(@TempDir tempDir: Path) {
        val prompt1 = """{"sessionId":"11111111-1111-1111-1111-111111111111","prompt":"Fix typo","maxActions":2}"""
        val prompt2 = """{"sessionId":"22222222-2222-2222-2222-222222222222","prompt":"Add test","maxActions":2}"""
        val input = "$prompt1\n$prompt2\n\n"

        val stdin = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        server.run(stdin, stdout)

        val output = stdout.toString(StandardCharsets.UTF_8).trim()
        val lines = output.lines().filter { it.isNotBlank() }

        assertEquals(2, lines.size)
        val mapper = jacksonObjectMapper()
        val r1: Map<String, Any> = mapper.readValue(lines[0])
        val r2: Map<String, Any> = mapper.readValue(lines[1])

        assertEquals("11111111-1111-1111-1111-111111111111", r1["sessionId"])
        assertEquals("22222222-2222-2222-2222-222222222222", r2["sessionId"])
    }

    @Test
    fun `generates sessionId when not provided`(@TempDir tempDir: Path) {
        val promptJson = """{"prompt":"Generate ID for me","maxActions":1}"""

        val stdin = ByteArrayInputStream("$promptJson\n\n".toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        server.run(stdin, stdout)

        val output = stdout.toString(StandardCharsets.UTF_8).trim()
        val mapper = jacksonObjectMapper()
        val response: Map<String, Any> = mapper.readValue(output)

        val sessionId = response["sessionId"] as String
        assertTrue(sessionId.isNotBlank(), "Session ID should be auto-generated")
        UUID.fromString(sessionId)
    }

    @Test
    fun `writes error response and continues on next prompt`(@TempDir tempDir: Path) {
        val failingPrompt = """{"sessionId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","prompt":"This should fail","maxActions":1}"""
        val okPrompt = """{"sessionId":"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb","prompt":"This should succeed","maxActions":2}"""
        val input = "$failingPrompt\n$okPrompt\n\n"

        val stdin = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val throwingProvider = object : LlmProvider {
            override suspend fun call(prompt: String): String {
                if (prompt.contains("fail")) {
                    throw RuntimeException("Simulated LLM failure")
                }
                return """{"intention":"$prompt","classification":"FEATURE","executedTasks":[],"finished":true}"""
            }
        }

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = throwingProvider
        )
        server.run(stdin, stdout)

        val output = stdout.toString(StandardCharsets.UTF_8).trim()
        val lines = output.lines().filter { it.isNotBlank() }

        assertEquals(2, lines.size)
        val mapper = jacksonObjectMapper()
        val r1: Map<String, Any> = mapper.readValue(lines[0])
        val r2: Map<String, Any> = mapper.readValue(lines[1])

        assertEquals("ERROR", r1["status"])
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", r1["sessionId"])
        assertTrue((r2["status"] as String) in listOf("COMPLETED", "IN_PROGRESS"))
        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", r2["sessionId"])
    }

    @Test
    fun `empty stdin returns immediately without output`(@TempDir tempDir: Path) {
        val stdin = ByteArrayInputStream("\n".toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        server.run(stdin, stdout)

        val output = stdout.toString(StandardCharsets.UTF_8).trim()
        assertTrue(output.isEmpty() || output.isBlank())
    }

    @Test
    fun `auto-loads governance context when no context provided`(@TempDir tempDir: Path) {
        val agentFile = tempDir.resolve("AGENT.adoc").toFile()
        agentFile.writeText("= Server Agent Rules\n\nRule 99: server governance loaded\n")

        val promptJson = """{"sessionId":"550e8400-e29b-41d4-a716-446655440000","prompt":"Server governance test","maxActions":1}"""
        val stdin = ByteArrayInputStream("$promptJson\n\n".toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = tempDir.toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        server.run(stdin, stdout)

        val ctx = server.lastAgentContext
        assertNotNull(ctx, "Server should auto-load governance context")
        assertTrue(ctx!!.eagerRules.contains("Rule 99: server governance loaded"),
            "Governance context should contain AGENT.adoc rules")
    }
}
