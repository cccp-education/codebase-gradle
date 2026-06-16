package codebase.koog.expert

import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import java.time.Duration

interface ExpertAgent {
    @SystemMessage(
        """
        {{systemPrompt}}
        """
    )
    @UserMessage(
        """
        Task type: {{request.subtaskType}}
        Expected output format: {{request.expectedOutputFormat}}
        Validation criteria: {{request.validationCriteria}}
        
        Context:
        {{request.context}}
        
        Prompt:
        {{request.prompt}}
        
        Return ONLY the requested output. No additional text.
        """
    )
    fun call(request: ExpertCallRequest): ExpertCallResponse
}

object ExpertAgentFactory {

    private val log = System.err

    fun create(
        registration: ExpertRegistration,
        systemPrompt: String
    ): ExpertAgent {
        return try {
            val model = OllamaChatModel.builder()
                .baseUrl(registration.baseUrl)
                .modelName(registration.modelName)
                .timeout(Duration.ofSeconds(registration.timeoutSeconds))
                .build()

            val agent = AiServices.builder(ExpertAgent::class.java)
                .chatModel(model)
                .systemMessageProvider { _ -> systemPrompt }
                .build()

            log.println("[EPIC-7] Expert agent created: ${registration.domain.name} @ ${registration.baseUrl}/${registration.modelName}")
            agent
        } catch (e: Exception) {
            log.println("[EPIC-7] Failed to create expert agent for ${registration.domain.name}: ${e.message?.take(80)}")
            throw e
        }
    }
}
