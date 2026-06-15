package codebase.koog

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.OutputStream
import java.io.PrintWriter
import java.time.Instant

enum class ToolEventType {
    THINKING,
    TOOL_CALL,
    TOOL_RESULT,
    PROGRESS,
    ERROR
}

data class ToolEvent(
    val type: ToolEventType,
    val timestamp: Instant = Instant.now(),
    val sessionId: String? = null,
    val iteration: Int = 0,
    val data: Map<String, String> = emptyMap()
)

class ToolEventStream(
    private val outputStream: OutputStream,
    sessionId: String? = null
) {
    private val writer = PrintWriter(outputStream, true, Charsets.UTF_8)
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    var currentSessionId: String? = sessionId

    fun emit(type: ToolEventType, iteration: Int = 0, data: Map<String, String> = emptyMap()) {
        val event = ToolEvent(
            type = type,
            sessionId = currentSessionId,
            iteration = iteration,
            data = data
        )
        val json = mapper.writeValueAsString(event)
        writer.println(json)
        writer.flush()
    }

    fun thinking(iteration: Int, message: String) {
        emit(ToolEventType.THINKING, iteration, mapOf("message" to message))
    }

    fun toolCall(iteration: Int, toolName: String, args: Map<String, String> = emptyMap()) {
        val data = mutableMapOf("tool" to toolName)
        args.forEach { (k, v) -> data["arg_$k"] = v }
        emit(ToolEventType.TOOL_CALL, iteration, data)
    }

    fun toolResult(iteration: Int, toolName: String, result: String, success: Boolean = true) {
        emit(ToolEventType.TOOL_RESULT, iteration, mapOf(
            "tool" to toolName,
            "success" to success.toString(),
            "result" to result.take(500)
        ))
    }

    fun progress(iteration: Int, maxActions: Int, message: String) {
        emit(ToolEventType.PROGRESS, iteration, mapOf(
            "iteration" to iteration.toString(),
            "maxActions" to maxActions.toString(),
            "message" to message
        ))
    }

    fun error(iteration: Int, message: String) {
        emit(ToolEventType.ERROR, iteration, mapOf("message" to message))
    }

    fun close() {
        writer.flush()
    }
}
