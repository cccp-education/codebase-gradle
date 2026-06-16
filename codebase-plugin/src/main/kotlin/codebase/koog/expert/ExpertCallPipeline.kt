package codebase.koog.expert

import codebase.rag.AnonymizationExpert
import codebase.rag.AnonymizationRequest
import codebase.rag.DeterministicExpert

class ExpertCallPipeline(
    private val dispatcher: DispatcherAgent,
    private val anonymizationExpert: AnonymizationExpert = DeterministicExpert
) {

    suspend fun execute(
        taskId: String,
        prompt: String,
        domainHints: List<ExpertDomain> = emptyList()
    ): ExpertCallPipelineResult {
        val anonymizedPrompt = anonymizePrompt(prompt)
        val dispatcherResult = dispatcher.execute(taskId, anonymizedPrompt, domainHints)

        return ExpertCallPipelineResult(
            taskId = taskId,
            originalPrompt = prompt,
            anonymizedPrompt = anonymizedPrompt,
            dispatcherResult = dispatcherResult
        )
    }

    private fun anonymizePrompt(prompt: String): String {
        return try {
            val request = AnonymizationRequest(
                sourcePath = "expert-pipeline",
                content = prompt,
                targetFormat = "text"
            )
            val result = anonymizationExpert.anonymizeRequest(request)
            if (result.replacedCount > 0) {
                System.err.println("[EPIC-7] Anonymized ${result.replacedCount} PII in prompt before dispatch")
            }
            result.anonymizedContent
        } catch (e: Exception) {
            System.err.println("[EPIC-7] Anonymization failed, using original prompt: ${e.message?.take(80)}")
            prompt
        }
    }

    data class ExpertCallPipelineResult(
        val taskId: String,
        val originalPrompt: String,
        val anonymizedPrompt: String,
        val dispatcherResult: DispatcherAgent.DispatcherResult
    )
}
