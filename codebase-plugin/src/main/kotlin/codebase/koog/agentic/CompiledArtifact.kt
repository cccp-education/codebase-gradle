package codebase.koog.agentic

enum class ArtifactType {
    PRE_HOOK,
    POST_HOOK,
    CI_GATE,
    GRADLE_TASK,
    VALIDATION,
    METADATA,
    PROMPT_TEMPLATE,
    CONSTRAINT_CHECK
}

data class CompiledArtifact(
    val sourceChunkId: String,
    val artifactType: ArtifactType,
    val description: String,
    val targetHint: String?,
    val confidence: Double,
    val payload: ArtifactPayload? = null
)
