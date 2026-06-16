package codebase.koog.expert

class FakeExpertAgent(
    private val domain: ExpertDomain,
    private val responseTemplate: String = "Expert response from {domain}"
) : ExpertAgent {
    var callCount = 0
        private set
    var lastRequest: ExpertCallRequest? = null
        private set

    override fun call(request: ExpertCallRequest): ExpertCallResponse {
        callCount++
        lastRequest = request
        return ExpertCallResponse(
            taskId = request.taskId,
            domain = domain,
            output = responseTemplate.replace("{domain}", domain.name),
            confidenceScore = 0.95,
            tokenUsage = ExpertTokenUsage(50, 100, 150),
            validationPassed = true
        )
    }
}

class ThrowingExpertAgent(
    private val errorMessage: String = "Expert unavailable"
) : ExpertAgent {
    override fun call(request: ExpertCallRequest): ExpertCallResponse {
        return ExpertCallResponse(
            taskId = request.taskId,
            domain = request.domain,
            output = "",
            confidenceScore = 0.0,
            tokenUsage = ExpertTokenUsage(0, 0, 0),
            validationPassed = false,
            error = errorMessage
        )
    }
}
