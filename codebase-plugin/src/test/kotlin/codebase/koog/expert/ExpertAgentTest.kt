package codebase.koog.expert

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ExpertAgentTest {

    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")

    @Test
    fun `FakeExpertAgent returns domain-specific response`() {
        val agent = FakeExpertAgent(kotlinDomain)
        val request = ExpertCallRequest(
            taskId = "task-001",
            domain = kotlinDomain,
            subtaskType = "code_generation",
            context = ExpertCallContext(),
            prompt = "Generate a Gradle plugin",
            expectedOutputFormat = "kotlin",
            validationCriteria = listOf("compiles")
        )

        val response = agent.call(request)

        assertEquals("task-001", response.taskId)
        assertEquals(kotlinDomain, response.domain)
        assertTrue(response.output.contains("kotlin"))
        assertEquals(0.95, response.confidenceScore)
        assertEquals(150, response.tokenUsage.totalTokens)
        assertTrue(response.validationPassed)
        assertNull(response.error)
    }

    @Test
    fun `FakeExpertAgent tracks call count`() {
        val agent = FakeExpertAgent(kotlinDomain)
        val request = ExpertCallRequest(
            taskId = "task-001",
            domain = kotlinDomain,
            subtaskType = "code_generation",
            context = ExpertCallContext(),
            prompt = "test",
            expectedOutputFormat = "kotlin",
            validationCriteria = emptyList()
        )

        assertEquals(0, agent.callCount)
        agent.call(request)
        assertEquals(1, agent.callCount)
        agent.call(request)
        assertEquals(2, agent.callCount)
    }

    @Test
    fun `FakeExpertAgent stores last request`() {
        val agent = FakeExpertAgent(kotlinDomain)
        val request = ExpertCallRequest(
            taskId = "task-002",
            domain = kotlinDomain,
            subtaskType = "refactoring",
            context = ExpertCallContext(maxTokens = 1000),
            prompt = "Refactor this class",
            expectedOutputFormat = "kotlin",
            validationCriteria = listOf("compiles", "no_regression")
        )

        agent.call(request)

        assertNotNull(agent.lastRequest)
        assertEquals("task-002", agent.lastRequest!!.taskId)
        assertEquals("refactoring", agent.lastRequest!!.subtaskType)
        assertEquals(1000, agent.lastRequest!!.context.maxTokens)
    }

    @Test
    fun `ThrowingExpertAgent returns failure response`() {
        val agent = ThrowingExpertAgent("Service unavailable")
        val request = ExpertCallRequest(
            taskId = "task-003",
            domain = docsDomain,
            subtaskType = "documentation",
            context = ExpertCallContext(),
            prompt = "Write docs",
            expectedOutputFormat = "asciidoc",
            validationCriteria = emptyList()
        )

        val response = agent.call(request)

        assertEquals("task-003", response.taskId)
        assertEquals(docsDomain, response.domain)
        assertTrue(response.output.isEmpty())
        assertEquals(0.0, response.confidenceScore)
        assertEquals(0, response.tokenUsage.totalTokens)
        assertFalse(response.validationPassed)
        assertEquals("Service unavailable", response.error)
    }

    @Test
    fun `FakeExpertAgent with custom template`() {
        val agent = FakeExpertAgent(docsDomain, "Documentation expert says: {domain}")
        val request = ExpertCallRequest(
            taskId = "task-004",
            domain = docsDomain,
            subtaskType = "documentation",
            context = ExpertCallContext(),
            prompt = "test",
            expectedOutputFormat = "asciidoc",
            validationCriteria = emptyList()
        )

        val response = agent.call(request)
        assertEquals("Documentation expert says: docs", response.output)
    }
}
