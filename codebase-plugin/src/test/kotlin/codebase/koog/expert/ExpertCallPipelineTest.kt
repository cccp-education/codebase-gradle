package codebase.koog.expert

import codebase.koog.llm.FakeLlmProvider
import codebase.rag.AnonymizationExpert
import codebase.rag.AnonymizationRequest
import codebase.rag.AnonymizationResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ExpertCallPipelineTest {

    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")

    private lateinit var registry: ExpertRegistry
    private lateinit var fakeDispatcherLlm: FakeLlmProvider
    private lateinit var fakeKotlinAgent: FakeExpertAgent
    private lateinit var fakeDocsAgent: FakeExpertAgent
    private lateinit var dispatcher: DispatcherAgent

    @BeforeEach
    fun setUp() {
        registry = ExpertRegistry()
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud"),
            ExpertRegistration(docsDomain, "gpt-oss:20b-cloud")
        ))

        fakeKotlinAgent = FakeExpertAgent(kotlinDomain, "Kotlin expert output for {domain}")
        fakeDocsAgent = FakeExpertAgent(docsDomain, "Docs expert output for {domain}")
        fakeDispatcherLlm = FakeLlmProvider()

        dispatcher = DispatcherAgent(
            dispatcherLlm = fakeDispatcherLlm,
            expertRegistry = registry,
            expertAgentFactory = { reg ->
                when (reg.domain.name) {
                    "kotlin" -> fakeKotlinAgent
                    "docs" -> fakeDocsAgent
                    else -> FakeExpertAgent(reg.domain)
                }
            }
        )
    }

    @Test
    fun `pipeline executes full flow with custom anonymization`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write a plugin","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val customAnonymizer = object : AnonymizationExpert {
            override fun anonymizeRequest(request: AnonymizationRequest): AnonymizationResult {
                return AnonymizationResult(
                    anonymizedContent = request.content.replace(Regex("sk-[a-zA-Z0-9]+"), "***"),
                    confidenceScore = 1.0,
                    detectedPiiCategories = listOf("api_key"),
                    replacedCount = 1,
                    summary = "Anonymized 1 API key"
                )
            }
        }

        val pipeline = ExpertCallPipeline(dispatcher, customAnonymizer)
        val result = pipeline.execute("task-001", "Build a Gradle plugin with API key: sk-1234567890abcdef")

        assertEquals("task-001", result.taskId)
        assertEquals("Build a Gradle plugin with API key: sk-1234567890abcdef", result.originalPrompt)
        assertNotEquals(result.originalPrompt, result.anonymizedPrompt)
        assertTrue(result.anonymizedPrompt.contains("***"))
        assertFalse(result.anonymizedPrompt.contains("sk-1234567890abcdef"))
        assertEquals(1, result.dispatcherResult.expertResponses.size)
        assertTrue(result.dispatcherResult.expertResponses[0].validationPassed)
    }

    @Test
    fun `pipeline with no PII passes prompt unchanged`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"docs","subtaskType":"documentation","prompt":"Write README","expectedOutputFormat":"asciidoc","validationCriteria":["complete"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val pipeline = ExpertCallPipeline(dispatcher)
        val cleanPrompt = "Write documentation for the project"
        val result = pipeline.execute("task-002", cleanPrompt)

        assertEquals(cleanPrompt, result.anonymizedPrompt)
    }

    @Test
    fun `pipeline with custom anonymization expert`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write code","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val customAnonymizer = object : AnonymizationExpert {
            override fun anonymizeRequest(request: AnonymizationRequest): AnonymizationResult {
                return AnonymizationResult(
                    anonymizedContent = "[ANONYMIZED] ${request.content}",
                    confidenceScore = 1.0,
                    detectedPiiCategories = listOf("custom"),
                    replacedCount = 1,
                    summary = "Custom anonymization"
                )
            }
        }

        val pipeline = ExpertCallPipeline(dispatcher, customAnonymizer)
        val result = pipeline.execute("task-003", "Some prompt with secrets")

        assertTrue(result.anonymizedPrompt.startsWith("[ANONYMIZED]"))
    }

    @Test
    fun `pipeline with multi-domain dispatch`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write plugin code","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1},
                {"id":"sub-2","domainName":"docs","subtaskType":"documentation","prompt":"Write README","expectedOutputFormat":"asciidoc","validationCriteria":["complete"],"priority":2}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val pipeline = ExpertCallPipeline(dispatcher)
        val result = pipeline.execute("task-004", "Build a complete Gradle plugin with docs")

        assertEquals(2, result.dispatcherResult.decomposition.subtasks.size)
        assertEquals(2, result.dispatcherResult.expertResponses.size)
        assertEquals(1, fakeKotlinAgent.callCount)
        assertEquals(1, fakeDocsAgent.callCount)
    }

    @Test
    fun `pipeline with domain hints`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"docs","subtaskType":"documentation","prompt":"Write docs","expectedOutputFormat":"asciidoc","validationCriteria":["complete"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val pipeline = ExpertCallPipeline(dispatcher)
        val result = pipeline.execute(
            taskId = "task-005",
            prompt = "Write documentation",
            domainHints = listOf(docsDomain)
        )

        assertEquals(1, result.dispatcherResult.expertResponses.size)
        assertEquals(docsDomain.name, result.dispatcherResult.expertResponses[0].domain.name)
    }

    @Test
    fun `pipeline preserves taskId through entire flow`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write code","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val pipeline = ExpertCallPipeline(dispatcher)
        val result = pipeline.execute("my-custom-task-id", "Write code")

        assertEquals("my-custom-task-id", result.taskId)
        assertEquals("my-custom-task-id", result.dispatcherResult.taskId)
    }
}
