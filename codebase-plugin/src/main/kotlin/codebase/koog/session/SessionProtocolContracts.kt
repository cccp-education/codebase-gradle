package codebase.koog.session

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class SessionPrompt(
    val sessionId: @Contextual UUID,
    val prompt: String,
    val context: String? = null,
    val maxActions: Int = 10,
    val model: String? = null
)

@Serializable
data class SessionResponse(
    val sessionId: @Contextual UUID,
    val output: String,
    val toolCalls: List<@Contextual ToolCallRecord> = emptyList(),
    val tokenUsage: TokenUsage? = null,
    val status: SessionStatus
)

@Serializable
enum class SessionStatus {
    COMPLETED,
    IN_PROGRESS,
    ERROR
}

@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val cost: Double = 0.0
)

@Serializable
data class ToolCallRecord(
    val toolName: String,
    val result: String,
    val timestamp: @Contextual Instant
)
