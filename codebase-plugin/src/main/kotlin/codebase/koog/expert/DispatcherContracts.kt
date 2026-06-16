package codebase.koog.expert

data class DispatcherSubtask(
    val id: String,
    val domainName: String,
    val subtaskType: String,
    val prompt: String,
    val expectedOutputFormat: String,
    val validationCriteria: List<String>,
    val priority: Int = 1
)

data class DispatcherDecomposition(
    val taskId: String,
    val originalPrompt: String,
    val subtasks: List<DispatcherSubtask>,
    val reasoning: String
)
