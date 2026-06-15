package codebase.koog

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class ToolEventStreamTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `emit produces valid JSON line`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out, "test-session")

        stream.emit(ToolEventType.THINKING, 1, mapOf("message" to "hello"))

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("THINKING", event["type"])
        assertEquals("test-session", event["sessionId"])
        assertEquals(1, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("hello", data["message"])
    }

    @Test
    fun `thinking helper emits THINKING event`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        stream.thinking(2, "Analyzing codebase")

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("THINKING", event["type"])
        assertEquals(2, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("Analyzing codebase", data["message"])
    }

    @Test
    fun `toolCall helper emits TOOL_CALL event with args`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out, "s1")

        stream.toolCall(3, "exec_gradle", mapOf("task" to "compileKotlin"))

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("TOOL_CALL", event["type"])
        assertEquals("s1", event["sessionId"])
        assertEquals(3, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("exec_gradle", data["tool"])
        assertEquals("compileKotlin", data["arg_task"])
    }

    @Test
    fun `toolResult helper emits TOOL_RESULT event`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        stream.toolResult(4, "exec_gradle", "BUILD SUCCESSFUL", success = true)

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("TOOL_RESULT", event["type"])
        assertEquals(4, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("exec_gradle", data["tool"])
        assertEquals("true", data["success"])
        assertTrue(data["result"]!!.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `toolResult truncates long results`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        val longResult = "x".repeat(2000)
        stream.toolResult(1, "exec_gradle", longResult)

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertTrue(data["result"]!!.length <= 500)
    }

    @Test
    fun `progress helper emits PROGRESS event`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        stream.progress(5, 10, "Halfway done")

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("PROGRESS", event["type"])
        assertEquals(5, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("5", data["iteration"])
        assertEquals("10", data["maxActions"])
        assertEquals("Halfway done", data["message"])
    }

    @Test
    fun `error helper emits ERROR event`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        stream.error(6, "Compilation failed")

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertEquals("ERROR", event["type"])
        assertEquals(6, event["iteration"])
        @Suppress("UNCHECKED_CAST")
        val data = event["data"] as Map<String, String>
        assertEquals("Compilation failed", data["message"])
    }

    @Test
    fun `multiple events produce multiple JSON lines`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out, "multi")

        stream.thinking(0, "Start")
        stream.toolCall(1, "exec_gradle", mapOf("task" to "build"))
        stream.toolResult(1, "exec_gradle", "OK")
        stream.progress(1, 3, "Step 1/3")
        stream.error(2, "Boom")

        val lines = out.toString(StandardCharsets.UTF_8).trim().lines().filter { it.isNotBlank() }
        assertEquals(5, lines.size)

        val types = lines.map { mapper.readValue<Map<String, Any>>(it)["type"] }
        assertEquals(listOf("THINKING", "TOOL_CALL", "TOOL_RESULT", "PROGRESS", "ERROR"), types)
    }

    @Test
    fun `currentSessionId can be changed between emits`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out, "first")

        stream.thinking(0, "Session 1")
        stream.currentSessionId = "second"
        stream.thinking(0, "Session 2")

        val lines = out.toString(StandardCharsets.UTF_8).trim().lines().filter { it.isNotBlank() }
        assertEquals(2, lines.size)

        val e1: Map<String, Any> = mapper.readValue(lines[0])
        val e2: Map<String, Any> = mapper.readValue(lines[1])

        assertEquals("first", e1["sessionId"])
        assertEquals("second", e2["sessionId"])
    }

    @Test
    fun `event has timestamp`() {
        val out = ByteArrayOutputStream()
        val stream = ToolEventStream(out)

        stream.emit(ToolEventType.PROGRESS, 0)

        val line = out.toString(StandardCharsets.UTF_8).trim()
        val event: Map<String, Any> = mapper.readValue(line)

        assertNotNull(event["timestamp"])
    }
}
