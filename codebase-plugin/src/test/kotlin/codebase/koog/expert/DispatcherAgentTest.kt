package codebase.koog.expert

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class DispatcherAgentTest {

    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")
    private val generalDomain = ExpertDomain("general", "Generalist fallback")

    private lateinit var registry: ExpertRegistry
    private lateinit var fakeDispatcherLlm: FakeLlmProvider
    private lateinit var fakeKotlinAgent: FakeExpertAgent
    private lateinit var fakeDocsAgent: FakeExpertAgent

    @BeforeEach
    fun setUp() {
        registry = ExpertRegistry()
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11449", 120),
            ExpertRegistration(docsDomain, "gpt-oss:120b-cloud", "http://localhost:11450", 90),
            ExpertRegistration(generalDomain, "gpt-oss:120b-cloud", "http://localhost:11451", 60)
        ))

        fakeKotlinAgent = FakeExpertAgent(kotlinDomain, "Kotlin expert output for {domain}")
        fakeDocsAgent = FakeExpertAgent(docsDomain, "Docs expert output for {domain}")

        fakeDispatcherLlm = FakeLlmProvider()
    }

    private fun createDispatcher(llm: LlmProvider = fakeDispatcherLlm): DispatcherAgent {
        return DispatcherAgent(
            dispatcherLlm = llm,
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
    fun `splitJsonArray parses single object`() {
        val json = """[{"id":"1","domainName":"kotlin"}]"""
        val items = DispatcherAgent.splitJsonArray(json)
        assertEquals(1, items.size)
        assertTrue(items[0].contains("kotlin"))
    }

    @Test
    fun `splitJsonArray parses multiple objects`() {
        val json = """[{"id":"1"},{"id":"2"},{"id":"3"}]"""
        val items = DispatcherAgent.splitJsonArray(json)
        assertEquals(3, items.size)
    }

    @Test
    fun `splitJsonArray handles nested objects`() {
        val json = """[{"id":"1","criteria":["a","b"]},{"id":"2"}]"""
        val items = DispatcherAgent.splitJsonArray(json)
        assertEquals(2, items.size)
    }

    @Test
    fun `extractJsonString finds value`() {
        val json = """{"id":"task-1","domainName":"kotlin"}"""
        assertEquals("task-1", DispatcherAgent.extractJsonString(json, "id"))
        assertEquals("kotlin", DispatcherAgent.extractJsonString(json, "domainName"))
    }

    @Test
    fun `extractJsonString returns null for missing key`() {
        val json = """{"id":"task-1"}"""
        assertNull(DispatcherAgent.extractJsonString(json, "domainName"))
    }

    @Test
    fun `extractJsonArray finds array values`() {
        val json = """{"validationCriteria":["compiles","no_hallucination"]}"""
        val criteria = DispatcherAgent.extractJsonArray(json, "validationCriteria")
        assertEquals(listOf("compiles", "no_hallucination"), criteria)
    }

    @Test
    fun `extractJsonArray returns empty for missing key`() {
        val json = """{"id":"task-1"}"""
        assertTrue(DispatcherAgent.extractJsonArray(json, "validationCriteria").isEmpty())
    }

    @Test
    fun `extractJsonInt finds integer`() {
        val json = """{"priority":3}"""
        assertEquals(3, DispatcherAgent.extractJsonInt(json, "priority"))
    }

    @Test
    fun `extractJsonInt returns null for missing key`() {
        val json = """{"id":"task-1"}"""
        assertNull(DispatcherAgent.extractJsonInt(json, "priority"))
    }

    @Test
    fun `decomposition with valid JSON dispatches to experts`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write a plugin","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1},
                {"id":"sub-2","domainName":"docs","subtaskType":"documentation","prompt":"Write README","expectedOutputFormat":"asciidoc","validationCriteria":["complete"],"priority":2}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val dispatcher = createDispatcher()
        val result = dispatcher.execute("task-001", "Build a Gradle plugin with documentation")

        assertEquals("task-001", result.taskId)
        assertEquals(2, result.decomposition.subtasks.size)
        assertEquals(2, result.expertResponses.size)
        assertTrue(result.expertResponses.all { it.validationPassed })
        assertEquals(1, fakeKotlinAgent.callCount)
        assertEquals(1, fakeDocsAgent.callCount)
    }

    @Test
    fun `decomposition with invalid JSON falls back to single subtask`() = runTest {
        fakeDispatcherLlm.nextResponse = "not valid json at all"

        val dispatcher = createDispatcher()
        val result = dispatcher.execute("task-002", "Some task")

        assertEquals(1, result.decomposition.subtasks.size)
        assertEquals("kotlin", result.decomposition.subtasks[0].domainName)
        assertEquals(1, result.expertResponses.size)
    }

    @Test
    fun `dispatch to unregistered domain returns error`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"unknown_domain","subtaskType":"analysis","prompt":"Analyze","expectedOutputFormat":"text","validationCriteria":[],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val dispatcher = createDispatcher()
        val result = dispatcher.execute("task-003", "Analyze something")

        assertEquals(1, result.expertResponses.size)
        val response = result.expertResponses[0]
        assertFalse(response.validationPassed)
        assertTrue(response.error!!.contains("No expert registered"))
    }

    @Test
    fun `synthesis when all experts succeed`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"kotlin","subtaskType":"code_generation","prompt":"Write code","expectedOutputFormat":"kotlin","validationCriteria":["compiles"],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val dispatcher = createDispatcher()
        val result = dispatcher.execute("task-004", "Write code")

        assertTrue(result.synthesis.isNotBlank())
        assertEquals(2, fakeDispatcherLlm.promptsReceived.size)
    }

    @Test
    fun `synthesis when all experts fail`() = runTest {
        val decompositionJson = """
            [
                {"id":"sub-1","domainName":"unknown","subtaskType":"analysis","prompt":"Analyze","expectedOutputFormat":"text","validationCriteria":[],"priority":1}
            ]
        """.trimIndent()
        fakeDispatcherLlm.nextResponse = decompositionJson

        val dispatcher = createDispatcher()
        val result = dispatcher.execute("task-005", "Analyze")

        assertTrue(result.synthesis.contains("failed"))
        assertTrue(result.synthesis.contains("all"))
    }

    @Test
    fun `DispatcherSubtask has correct defaults`() {
        val subtask = DispatcherSubtask(
            id = "sub-1",
            domainName = "kotlin",
            subtaskType = "code_generation",
            prompt = "test",
            expectedOutputFormat = "kotlin",
            validationCriteria = emptyList()
        )
        assertEquals(1, subtask.priority)
    }

    @Test
    fun `DispatcherDecomposition holds all fields`() {
        val subtasks = listOf(
            DispatcherSubtask("sub-1", "kotlin", "code", "test", "kotlin", emptyList())
        )
        val decomposition = DispatcherDecomposition(
            taskId = "task-001",
            originalPrompt = "Build plugin",
            subtasks = subtasks,
            reasoning = "Single domain task"
        )

        assertEquals("task-001", decomposition.taskId)
        assertEquals("Build plugin", decomposition.originalPrompt)
        assertEquals(1, decomposition.subtasks.size)
        assertEquals("Single domain task", decomposition.reasoning)
    }
}
