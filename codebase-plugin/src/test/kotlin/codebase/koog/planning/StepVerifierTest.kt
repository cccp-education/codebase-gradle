package codebase.koog.planning

import codebase.koog.llm.LlmProvider
import codebase.koog.state.VibecodingState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StepVerifierTest {

    private val verifier = StepVerifier()

    @Test
    fun `SUCCESS verdict should clear error`() {
        val state = vibecodingState(lastToolResult = "BUILD SUCCESSFUL in 5s")
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL")
        val result = verifier.verifyAndAdapt(state, step)
        assertNull(result.error)
    }

    @Test
    fun `FAILED verdict with retries remaining should retry`() {
        val state = vibecodingState(
            lastToolResult = "BUILD FAILED",
            retryCount = 0,
            maxRetries = 3
        )
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL", maxRetries = 3)
        val result = verifier.verifyAndAdapt(state, step)
        assertEquals(1, result.retryCount)
        assertNull(result.error)
    }

    @Test
    fun `FAILED verdict with maxRetries exhausted should set error`() {
        val state = vibecodingState(
            lastToolResult = "BUILD FAILED",
            retryCount = 3,
            maxRetries = 3
        )
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL", maxRetries = 3)
        val result = verifier.verifyAndAdapt(state, step)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("MaxRetriesExhausted"))
    }

    @Test
    fun `BLOCKED verdict should set blocked error`() {
        val state = vibecodingState(lastToolResult = "Task 'generateSPD' not found in project", retryCount = 0)
        val step = VibecodingStep("generateSPD", "generateSPD", "SPD generated")
        val result = verifier.verifyAndAdapt(state, step)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("BLOCKED"))
    }

    @Test
    fun `UNKNOWN verdict should set unknown error`() {
        val state = vibecodingState(lastToolResult = "Some random output", retryCount = 0)
        val step = VibecodingStep("mystery", "mysteryTask", "Expected output")
        val result = verifier.verifyAndAdapt(state, step)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("UNKNOWN"))
    }

    @Test
    fun `FAILED with LLM provider should replan`() {
        val fakeLlm = LlmProvider { "Try ./gradlew compileKotlin instead" }
        val verifierWithLlm = StepVerifier(llmProvider = fakeLlm)
        val state = vibecodingState(
            lastToolResult = "BUILD FAILED\ncompilation error",
            retryCount = 0,
            maxRetries = 3
        )
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL", maxRetries = 3)
        val result = verifierWithLlm.verifyAndAdapt(state, step)
        assertEquals(1, result.retryCount)
        assertNull(result.error)
        assertTrue(result.lastToolResult.contains("Replan"))
    }

    @Test
    fun `FAILED without LLM provider should just retry`() {
        val state = vibecodingState(
            lastToolResult = "BUILD FAILED",
            retryCount = 0,
            maxRetries = 3
        )
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL", maxRetries = 3)
        val result = verifier.verifyAndAdapt(state, step)
        assertEquals(1, result.retryCount)
        assertNull(result.error)
    }

    private fun vibecodingState(
        lastToolResult: String = "",
        retryCount: Int = 0,
        maxRetries: Int = 3,
        executedTasks: List<String> = emptyList()
    ) = VibecodingState(
        intention = "test",
        workspaceRoot = "/tmp",
        lastToolResult = lastToolResult,
        retryCount = retryCount,
        maxRetries = maxRetries,
        executedTasks = executedTasks
    )
}
