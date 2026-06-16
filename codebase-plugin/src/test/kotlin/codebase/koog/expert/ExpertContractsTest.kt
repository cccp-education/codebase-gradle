package codebase.koog.expert

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ExpertContractsTest {

    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")
    private val generalDomain = ExpertDomain("general", "Generalist fallback")

    @Test
    fun `ExpertDomain is a data class with name and label`() {
        assertEquals("kotlin", kotlinDomain.name)
        assertEquals("Kotlin, Gradle, JVM ecosystem", kotlinDomain.label)
        assertEquals(kotlinDomain, ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem"))
    }

    @Test
    fun `ExpertCallRequest serialization roundtrip`() {
        val request = ExpertCallRequest(
            taskId = "task-001",
            domain = kotlinDomain,
            subtaskType = "code_generation",
            context = ExpertCallContext(
                maxTokens = 2000,
                relevantSchemas = listOf("plugin.json"),
                relevantFiles = listOf("build.gradle.kts")
            ),
            prompt = "Generate a Kotlin class for a Gradle plugin",
            expectedOutputFormat = "kotlin",
            validationCriteria = listOf("compiles", "matches_schema", "no_hallucination")
        )

        assertEquals("task-001", request.taskId)
        assertEquals(kotlinDomain, request.domain)
        assertEquals("code_generation", request.subtaskType)
        assertEquals(2000, request.context.maxTokens)
        assertEquals(listOf("plugin.json"), request.context.relevantSchemas)
        assertEquals(listOf("build.gradle.kts"), request.context.relevantFiles)
        assertEquals("Generate a Kotlin class for a Gradle plugin", request.prompt)
        assertEquals("kotlin", request.expectedOutputFormat)
        assertEquals(3, request.validationCriteria.size)
    }

    @Test
    fun `ExpertCallResponse with success`() {
        val response = ExpertCallResponse(
            taskId = "task-001",
            domain = kotlinDomain,
            output = "class MyPlugin : Plugin<Project> { }",
            confidenceScore = 0.95,
            tokenUsage = ExpertTokenUsage(150, 300, 450),
            validationPassed = true
        )

        assertEquals("task-001", response.taskId)
        assertEquals(kotlinDomain, response.domain)
        assertTrue(response.output.contains("MyPlugin"))
        assertEquals(0.95, response.confidenceScore)
        assertEquals(150, response.tokenUsage.promptTokens)
        assertEquals(300, response.tokenUsage.completionTokens)
        assertEquals(450, response.tokenUsage.totalTokens)
        assertTrue(response.validationPassed)
        assertNull(response.error)
    }

    @Test
    fun `ExpertCallResponse with failure`() {
        val response = ExpertCallResponse(
            taskId = "task-002",
            domain = docsDomain,
            output = "",
            confidenceScore = 0.0,
            tokenUsage = ExpertTokenUsage(100, 0, 100),
            validationPassed = false,
            error = "Off-topic detected: response not about documentation"
        )

        assertFalse(response.validationPassed)
        assertEquals("Off-topic detected: response not about documentation", response.error)
        assertTrue(response.output.isEmpty())
    }

    @Test
    fun `ExpertCallContext defaults`() {
        val defaultContext = ExpertCallContext()
        assertEquals(2000, defaultContext.maxTokens)
        assertTrue(defaultContext.relevantSchemas.isEmpty())
        assertTrue(defaultContext.relevantFiles.isEmpty())
    }

    @Test
    fun `ExpertDomain equality and hash`() {
        val a = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
        val b = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
        val c = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun `ExpertTokenUsage totals are consistent`() {
        val usage = ExpertTokenUsage(100, 200, 300)
        assertEquals(300, usage.promptTokens + usage.completionTokens)
        assertEquals(300, usage.totalTokens)
    }
}
