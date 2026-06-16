package codebase.koog.expert

import java.time.Instant

data class ExpertCallRecord(
    val id: String,
    val taskId: String,
    val domainName: String,
    val subtaskType: String,
    val prompt: String,
    val anonymizedPrompt: String,
    val output: String,
    val confidenceScore: Double,
    val promptTokens: Int,
    val completionTokens: Int,
    val validationPassed: Boolean,
    val error: String?,
    val createdAt: Instant = Instant.now()
)
