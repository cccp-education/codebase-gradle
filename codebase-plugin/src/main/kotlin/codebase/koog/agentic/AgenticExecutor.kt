package codebase.koog.agentic

class AgenticExecutor(
    executables: List<ExecutableArtifact> = emptyList()
) {

    private val hookArtifacts = executables.filter {
        it.compiledArtifact.artifactType == ArtifactType.PRE_HOOK
    }

    fun check(toolName: String, arguments: Map<String, String>): ExecutionResult {
        for (artifact in hookArtifacts) {
            val result = artifact.execute(toolName, arguments)
            if (!result.allowed) return result
        }
        return ExecutionResult(allowed = true)
    }
}
