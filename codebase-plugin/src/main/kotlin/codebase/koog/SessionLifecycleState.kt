package codebase.koog

data class SessionLifecycleState(
    val sessionId: String,
    val prompt: String,
    val model: String?,
    val status: LifecycleStatus,
    val parentSessionId: String?,
    val lastResponseJson: String?,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant
)

enum class LifecycleStatus {
    CREATED,
    RUNNING,
    CLOSED
}
