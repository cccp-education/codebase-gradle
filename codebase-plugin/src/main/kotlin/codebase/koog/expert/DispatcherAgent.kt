package codebase.koog.expert

import codebase.koog.llm.LlmProvider

class DispatcherAgent(
    private val dispatcherLlm: LlmProvider,
    private val expertRegistry: ExpertRegistry,
    private val expertAgentFactory: (ExpertRegistration) -> ExpertAgent
) {

    suspend fun execute(
        taskId: String,
        prompt: String,
        domainHints: List<ExpertDomain> = expertRegistry.listDomains()
    ): DispatcherResult {
        val decomposition = decompose(taskId, prompt, domainHints)
        val responses = dispatchSubtasks(decomposition)
        val synthesis = synthesize(taskId, prompt, responses)

        return DispatcherResult(
            taskId = taskId,
            decomposition = decomposition,
            expertResponses = responses,
            synthesis = synthesis
        )
    }

    private suspend fun decompose(
        taskId: String,
        prompt: String,
        domains: List<ExpertDomain>
    ): DispatcherDecomposition {
        val domainList = domains.joinToString("\n") { "- ${it.name}: ${it.label}" }
        val decompositionPrompt = """
            You are a task dispatcher. Decompose the following complex task into subtasks,
            assigning each to the most appropriate expert domain from the available list.
            
            Available expert domains:
            $domainList
            
            Task to decompose:
            $prompt
            
            Return a JSON array of subtasks. Each subtask must have:
            - "id": unique string identifier
            - "domainName": one of the available domain names
            - "subtaskType": type of work (code_generation, documentation, analysis, etc.)
            - "prompt": the specific prompt for this subtask
            - "expectedOutputFormat": kotlin, asciidoc, json, text, etc.
            - "validationCriteria": list of validation checks
            - "priority": integer (1=highest)
            
            Return ONLY the JSON array. No other text.
        """.trimIndent()

        val rawResponse = dispatcherLlm.call(decompositionPrompt)
        return parseDecomposition(taskId, prompt, rawResponse, domains)
    }

    private fun parseDecomposition(
        taskId: String,
        originalPrompt: String,
        rawResponse: String,
        domains: List<ExpertDomain>
    ): DispatcherDecomposition {
        val subtasks = try {
            val jsonStart = rawResponse.indexOf('[')
            val jsonEnd = rawResponse.lastIndexOf(']') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonArray = rawResponse.substring(jsonStart, jsonEnd)
                parseSubtasksFromJson(jsonArray, domains)
            } else {
                fallbackSingleSubtask(taskId, originalPrompt, domains)
            }
        } catch (e: Exception) {
            fallbackSingleSubtask(taskId, originalPrompt, domains)
        }

        return DispatcherDecomposition(
            taskId = taskId,
            originalPrompt = originalPrompt,
            subtasks = subtasks,
            reasoning = "Decomposed into ${subtasks.size} subtask(s)"
        )
    }

    private fun parseSubtasksFromJson(
        jsonArray: String,
        domains: List<ExpertDomain>
    ): List<DispatcherSubtask> {
        val domainMap = domains.associateBy { it.name }
        val results = mutableListOf<DispatcherSubtask>()

        val items = splitJsonArray(jsonArray)
        for (item in items) {
            val id = extractJsonString(item, "id") ?: "subtask-${results.size}"
            val domainName = extractJsonString(item, "domainName") ?: domains.firstOrNull()?.name ?: "general"
            val subtaskType = extractJsonString(item, "subtaskType") ?: "general"
            val prompt = extractJsonString(item, "prompt") ?: ""
            val expectedFormat = extractJsonString(item, "expectedOutputFormat") ?: "text"
            val criteria = extractJsonArray(item, "validationCriteria")
            val priority = extractJsonInt(item, "priority") ?: 1

            if (prompt.isNotBlank()) {
                results.add(
                    DispatcherSubtask(
                        id = id,
                        domainName = domainName,
                        subtaskType = subtaskType,
                        prompt = prompt,
                        expectedOutputFormat = expectedFormat,
                        validationCriteria = criteria,
                        priority = priority
                    )
                )
            }
        }

        return results.ifEmpty {
            fallbackSingleSubtask("fallback", "", domains)
        }
    }

    private fun fallbackSingleSubtask(
        taskId: String,
        prompt: String,
        domains: List<ExpertDomain>
    ): List<DispatcherSubtask> {
        val defaultDomain = domains.firstOrNull()?.name ?: "general"
        return listOf(
            DispatcherSubtask(
                id = "$taskId-0",
                domainName = defaultDomain,
                subtaskType = "general",
                prompt = prompt,
                expectedOutputFormat = "text",
                validationCriteria = emptyList(),
                priority = 1
            )
        )
    }

    private suspend fun dispatchSubtasks(
        decomposition: DispatcherDecomposition
    ): List<ExpertCallResponse> {
        return decomposition.subtasks.map { subtask ->
            val domain = ExpertDomain(subtask.domainName, "")
            val registration = expertRegistry.resolveByName(subtask.domainName)

            if (registration == null) {
                ExpertCallResponse(
                    taskId = subtask.id,
                    domain = domain,
                    output = "",
                    confidenceScore = 0.0,
                    tokenUsage = ExpertTokenUsage(0, 0, 0),
                    validationPassed = false,
                    error = "No expert registered for domain: ${subtask.domainName}"
                )
            } else {
                val agent = expertAgentFactory(registration)
                val request = ExpertCallRequest(
                    taskId = subtask.id,
                    domain = domain,
                    subtaskType = subtask.subtaskType,
                    context = ExpertCallContext(),
                    prompt = subtask.prompt,
                    expectedOutputFormat = subtask.expectedOutputFormat,
                    validationCriteria = subtask.validationCriteria
                )
                agent.call(request)
            }
        }
    }

    private suspend fun synthesize(
        taskId: String,
        originalPrompt: String,
        responses: List<ExpertCallResponse>
    ): String {
        val successful = responses.filter { it.validationPassed }
        val failed = responses.filter { !it.validationPassed }

        if (successful.isEmpty()) {
            return "Synthesis failed: all ${responses.size} expert calls failed. " +
                failed.joinToString("; ") { "${it.domain.name}: ${it.error}" }
        }

        val expertOutputs = successful.joinToString("\n\n") { response ->
            "=== Expert: ${response.domain.name} ===\n${response.output}"
        }

        val synthesisPrompt = """
            You are a synthesis agent. Combine the following expert outputs into a coherent,
            unified response for the original task.
            
            Original task: $originalPrompt
            
            Expert outputs:
            $expertOutputs
            
            ${if (failed.isNotEmpty()) "Note: ${failed.size} expert(s) failed: ${failed.joinToString { it.domain.name }}" else ""}
            
            Synthesize a single, coherent response. Return ONLY the synthesis.
        """.trimIndent()

        return try {
            dispatcherLlm.call(synthesisPrompt)
        } catch (e: Exception) {
            successful.joinToString("\n\n") { it.output }
        }
    }

    data class DispatcherResult(
        val taskId: String,
        val decomposition: DispatcherDecomposition,
        val expertResponses: List<ExpertCallResponse>,
        val synthesis: String
    )

    companion object {
        internal fun splitJsonArray(json: String): List<String> {
            val items = mutableListOf<String>()
            var depth = 0
            val current = StringBuilder()
            var inString = false
            var escape = false

            for (char in json) {
                when {
                    escape -> { current.append(char); escape = false }
                    char == '\\' -> { current.append(char); escape = true }
                    char == '"' -> { inString = !inString; current.append(char) }
                    !inString && char == '{' -> { depth++; current.append(char) }
                    !inString && char == '}' -> {
                        depth--
                        current.append(char)
                        if (depth == 0) {
                            items.add(current.toString().trim())
                            current.clear()
                        }
                    }
                    else -> current.append(char)
                }
            }
            return items
        }

        internal fun extractJsonString(json: String, key: String): String? {
            val regex = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
            return regex.find(json)?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\\\", "\\")
        }

        internal fun extractJsonArray(json: String, key: String): List<String> {
            val regex = Regex("\"$key\"\\s*:\\s*\\[([^]]*)\\]")
            val match = regex.find(json)?.groupValues?.get(1) ?: return emptyList()
            return Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(match)
                .map { it.groupValues[1].replace("\\\"", "\"") }
                .toList()
        }

        internal fun extractJsonInt(json: String, key: String): Int? {
            val regex = Regex("\"$key\"\\s*:\\s*(\\d+)")
            return regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
        }
    }
}
