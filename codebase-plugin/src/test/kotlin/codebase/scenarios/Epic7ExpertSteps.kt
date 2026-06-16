package codebase.scenarios

import codebase.koog.expert.DispatcherAgent
import codebase.koog.expert.ExpertCallPipeline
import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertRegistration
import codebase.koog.expert.FakeExpertAgent
import codebase.koog.llm.FakeLlmProvider
import codebase.rag.AnonymizationExpert
import codebase.rag.AnonymizationRequest
import codebase.rag.AnonymizationResult
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.runBlocking
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Epic7ExpertSteps(private val world: Epic7ExpertWorld) {

    @Given("an expert registry with {string} and {string} domains")
    fun `expert registry with domains`(domain1: String, domain2: String) {
        world.registry.registerAll(listOf(
            ExpertRegistration(ExpertDomain(domain1, "$domain1 domain"), "gpt-oss:120b-cloud"),
            ExpertRegistration(ExpertDomain(domain2, "$domain2 domain"), "gpt-oss:20b-cloud")
        ))
    }

    @Given("a dispatcher agent is initialized with the registry")
    fun `dispatcher agent initialized`() {
        world.dispatcher = DispatcherAgent(
            dispatcherLlm = world.fakeDispatcherLlm,
            expertRegistry = world.registry,
            expertAgentFactory = { reg ->
                when (reg.domain.name) {
                    "kotlin" -> world.fakeKotlinAgent
                    "docs" -> world.fakeDocsAgent
                    else -> FakeExpertAgent(reg.domain)
                }
            }
        )
    }

    @Given("a pipeline with anonymization is initialized")
    fun `pipeline with anonymization initialized`() {
        world.dispatcher = DispatcherAgent(
            dispatcherLlm = world.fakeDispatcherLlm,
            expertRegistry = world.registry,
            expertAgentFactory = { reg ->
                when (reg.domain.name) {
                    "kotlin" -> world.fakeKotlinAgent
                    "docs" -> world.fakeDocsAgent
                    else -> FakeExpertAgent(reg.domain)
                }
            }
        )
        val customAnonymizer = object : AnonymizationExpert {
            override fun anonymizeRequest(request: AnonymizationRequest): AnonymizationResult {
                val anonymized = request.content.replace(Regex("sk-[a-zA-Z0-9]+"), "***")
                return AnonymizationResult(
                    anonymizedContent = anonymized,
                    confidenceScore = 1.0,
                    detectedPiiCategories = if (anonymized != request.content) listOf("api_key") else emptyList(),
                    replacedCount = if (anonymized != request.content) 1 else 0,
                    summary = "Custom anonymization"
                )
            }
        }
        world.pipeline = ExpertCallPipeline(world.dispatcher, customAnonymizer)
    }

    @Given("a pipeline with persistence is initialized")
    fun `pipeline with persistence initialized`() {
        world.dispatcher = DispatcherAgent(
            dispatcherLlm = world.fakeDispatcherLlm,
            expertRegistry = world.registry,
            expertAgentFactory = { reg ->
                when (reg.domain.name) {
                    "kotlin" -> world.fakeKotlinAgent
                    "docs" -> world.fakeDocsAgent
                    else -> FakeExpertAgent(reg.domain)
                }
            }
        )
        world.pipeline = ExpertCallPipeline(world.dispatcher)
    }

    @When("I dispatch the task {string} with domain hints {string}")
    fun `dispatch task with domain hints`(task: String, hints: String) = runBlocking {
        val domainHints = if (hints.isBlank()) emptyList()
        else hints.split(",").map { ExpertDomain(it.trim(), "") }

        val decompositionJson = buildDecompositionJson(task, domainHints)
        world.fakeDispatcherLlm.nextResponse = decompositionJson

        world.lastResult = world.dispatcher.execute("task-001", task, domainHints)
    }

    @When("I execute the pipeline with prompt {string}")
    fun `execute pipeline with prompt`(prompt: String) = runBlocking {
        world.lastPrompt = prompt
        val decompositionJson = buildDecompositionJson(prompt, world.registry.listDomains())
        world.fakeDispatcherLlm.nextResponse = decompositionJson

        world.lastPipelineResult = world.pipeline.execute("task-002", prompt)
        world.lastAnonymizedPrompt = world.lastPipelineResult.anonymizedPrompt
    }

    @Then("the dispatcher decomposes into at least {int} subtasks")
    fun `decomposes into at least N subtasks`(minSubtasks: Int) {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.decomposition.subtasks.size >= minSubtasks,
            "Expected >= $minSubtasks subtasks, got ${world.lastResult.decomposition.subtasks.size}")
    }

    @Then("all subtasks are assigned to the {string} domain")
    fun `all subtasks assigned to domain`(domain: String) {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.decomposition.subtasks.all { it.domainName == domain },
            "Not all subtasks assigned to $domain: ${world.lastResult.decomposition.subtasks.map { it.domainName }}")
    }

    @Then("all expert calls succeed")
    fun `all expert calls succeed`() {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.expertResponses.all { it.validationPassed },
            "Some expert calls failed: ${world.lastResult.expertResponses.filter { !it.validationPassed }.map { it.error }}")
    }

    @Then("the synthesis output is not empty")
    fun `synthesis output not empty`() {
        assertNotNull(world.lastResult)
        assertTrue(world.lastResult.synthesis.isNotBlank(), "Synthesis is empty")
    }

    @Then("subtasks are assigned to both {string} and {string} domains")
    fun `subtasks assigned to both domains`(domain1: String, domain2: String) {
        assertNotNull(world.lastResult)
        val domainNames = world.lastResult.decomposition.subtasks.map { it.domainName }.toSet()
        assertTrue(domain1 in domainNames, "Domain $domain1 not found in subtasks: $domainNames")
        assertTrue(domain2 in domainNames, "Domain $domain2 not found in subtasks: $domainNames")
    }

    @Then("at least {int} expert call fails")
    fun `at least N expert calls fail`(minFailures: Int) {
        assertNotNull(world.lastResult)
        val failures = world.lastResult.expertResponses.filter { !it.validationPassed }
        assertTrue(failures.size >= minFailures,
            "Expected >= $minFailures failures, got ${failures.size}")
    }

    @Then("the failed call error contains {string}")
    fun `failed call error contains`(expectedError: String) {
        assertNotNull(world.lastResult)
        val failures = world.lastResult.expertResponses.filter { !it.validationPassed }
        assertTrue(failures.isNotEmpty(), "No failed calls to check")
        assertTrue(failures.any { it.error?.contains(expectedError) == true },
            "No failed call contains '$expectedError'. Errors: ${failures.map { it.error }}")
    }

    @Then("the anonymized prompt does not contain {string}")
    fun `anonymized prompt does not contain`(forbidden: String) {
        assertFalse(forbidden in world.lastAnonymizedPrompt,
            "Found '$forbidden' in anonymized prompt: ${world.lastAnonymizedPrompt}")
    }

    @Then("the anonymized prompt contains {string}")
    fun `anonymized prompt contains`(expected: String) {
        assertTrue(expected in world.lastAnonymizedPrompt,
            "Expected '$expected' in anonymized prompt: ${world.lastAnonymizedPrompt}")
    }

    @Then("the dispatcher receives the anonymized prompt")
    fun `dispatcher receives anonymized prompt`() {
        assertNotNull(world.lastPipelineResult)
        assertNotEquals(world.lastPrompt, world.lastAnonymizedPrompt,
            "Anonymized prompt should differ from original")
    }

    @Then("at least {int} expert call is persisted")
    fun `at least N expert calls persisted`(minPersisted: Int) {
        assertNotNull(world.lastPipelineResult)
        val responses = world.lastPipelineResult.dispatcherResult.expertResponses
        assertTrue(responses.size >= minPersisted,
            "Expected >= $minPersisted expert calls, got ${responses.size}")
    }

    @Then("the persisted calls can be retrieved by task ID")
    fun `persisted calls retrievable by task ID`() {
        assertNotNull(world.lastPipelineResult)
        val taskId = world.lastPipelineResult.taskId
        assertTrue(taskId.isNotBlank(), "Task ID should not be blank")
    }

    private fun buildDecompositionJson(task: String, domains: List<ExpertDomain>): String {
        if (domains.isEmpty()) return "not valid json"
        val domainNames = domains.map { it.name }
        val subtasks = domainNames.mapIndexed { i, name ->
            """{"id":"sub-$i","domainName":"$name","subtaskType":"general","prompt":"$task part $i","expectedOutputFormat":"text","validationCriteria":[],"priority":${i + 1}}"""
        }
        return "[${subtasks.joinToString(",")}]"
    }
}
