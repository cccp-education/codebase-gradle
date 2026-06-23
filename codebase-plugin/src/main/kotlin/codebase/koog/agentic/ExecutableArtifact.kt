package codebase.koog.agentic

sealed class ArtifactPayload {
    abstract val toolName: String?

    data class PreHookPayload(
        override val toolName: String,
        val forbiddenPatterns: List<String>,
        val allowedPattern: String? = null
    ) : ArtifactPayload()

    data class GradleTaskPayload(
        override val toolName: String? = "exec_gradle",
        val taskName: String,
        val group: String? = null,
        val description: String? = null
    ) : ArtifactPayload()

    data class ValidationPayload(
        override val toolName: String? = "exec_gradle",
        val checkDescription: String
    ) : ArtifactPayload()

    data class ConstraintPayload(
        override val toolName: String? = "exec_gradle",
        val constraintDescription: String,
        val maxTokens: Int? = null,
        val maxLines: Int? = null
    ) : ArtifactPayload()

    data class MetadataPayload(
        override val toolName: String? = null,
        val metadataKey: String,
        val metadataValue: String
    ) : ArtifactPayload()

    data class PromptTemplatePayload(
        override val toolName: String? = null,
        val templateName: String,
        val promptText: String
    ) : ArtifactPayload()
}

data class ExecutionResult(
    val allowed: Boolean,
    val ruleId: String? = null,
    val reason: String? = null
)

data class ExecutableArtifact(
    val compiledArtifact: CompiledArtifact,
    val payload: ArtifactPayload
) {
    fun execute(toolName: String, arguments: Map<String, String>): ExecutionResult {
        return when (payload) {
            is ArtifactPayload.PreHookPayload -> executePreHook(toolName, arguments, payload)
            is ArtifactPayload.ConstraintPayload,
            is ArtifactPayload.ValidationPayload -> executeValidation(toolName, arguments, payload)
            else -> ExecutionResult(allowed = true)
        }
    }

    private fun executePreHook(
        toolName: String,
        arguments: Map<String, String>,
        payload: ArtifactPayload.PreHookPayload
    ): ExecutionResult {
        if (payload.toolName != toolName) return ExecutionResult(allowed = true)

        val taskArg = arguments["task"] ?: arguments["command"] ?: return ExecutionResult(allowed = true)

        for (pattern in payload.forbiddenPatterns) {
            val forbiddenMatches = pattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(taskArg)
            if (!forbiddenMatches) continue

            if (payload.allowedPattern != null) {
                val allowedMatches = payload.allowedPattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(taskArg)
                if (allowedMatches) continue
            }

            return ExecutionResult(
                allowed = false,
                ruleId = compiledArtifact.sourceChunkId,
                reason = compiledArtifact.description
            )
        }

        return ExecutionResult(allowed = true)
    }

    private fun executeValidation(
        toolName: String,
        arguments: Map<String, String>,
        payload: ArtifactPayload
    ): ExecutionResult {
        val expectedTool = when (payload) {
            is ArtifactPayload.ValidationPayload -> payload.toolName
            is ArtifactPayload.ConstraintPayload -> payload.toolName
            else -> null
        }
        if (expectedTool != null && expectedTool != toolName) {
            return ExecutionResult(allowed = true)
        }

        val taskArg = arguments["task"] ?: arguments["command"] ?: return ExecutionResult(allowed = true)
        val description = when (payload) {
            is ArtifactPayload.ValidationPayload -> payload.checkDescription
            is ArtifactPayload.ConstraintPayload -> payload.constraintDescription
            else -> ""
        }

        return if (taskArg.contains(description, ignoreCase = true)) {
            ExecutionResult(allowed = true)
        } else {
            ExecutionResult(allowed = false, ruleId = compiledArtifact.sourceChunkId, reason = description)
        }
    }
}
