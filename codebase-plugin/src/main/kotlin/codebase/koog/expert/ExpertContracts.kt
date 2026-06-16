package codebase.koog.expert

data class ExpertDomain(
    val name: String,
    val label: String
)

data class ExpertCallRequest(
    val taskId: String,
    val domain: ExpertDomain,
    val subtaskType: String,
    val context: ExpertCallContext,
    val prompt: String,
    val expectedOutputFormat: String,
    val validationCriteria: List<String>
)

data class ExpertCallContext(
    val maxTokens: Int = 2000,
    val relevantSchemas: List<String> = emptyList(),
    val relevantFiles: List<String> = emptyList()
)

data class ExpertCallResponse(
    val taskId: String,
    val domain: ExpertDomain,
    val output: String,
    val confidenceScore: Double,
    val tokenUsage: ExpertTokenUsage,
    val validationPassed: Boolean,
    val error: String? = null
)

data class ExpertTokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
