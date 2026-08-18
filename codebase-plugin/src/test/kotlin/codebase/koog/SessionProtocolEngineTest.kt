package codebase.koog

import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.session.AgentContext
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class SessionProtocolEngineTest {

    @Test
    fun `executeVibecoding returns SessionResponse with provided sessionId`(@TempDir tempDir: Path) {
        val engine = SessionProtocolEngine(
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        val sid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

        val response = engine.executeVibecoding(
            promptText = "Add dark mode",
            workspaceRootPath = tempDir.toString(),
            maxActions = 3,
            sessionId = sid,
            agentContext = AgentContext(),
            model = null
        )

        assertEquals(sid, response.sessionId)
        assertTrue(response.output.contains("Add dark mode"))
        assertTrue(response.status in listOf(SessionStatus.COMPLETED, SessionStatus.IN_PROGRESS))
    }

    @Test
    fun `executeVibecoding returns ERROR status when LLM throws`(@TempDir tempDir: Path) {
        val throwingProvider = object : LlmProvider {
            override suspend fun call(prompt: String): String {
                throw RuntimeException("Simulated LLM failure")
            }
        }
        val engine = SessionProtocolEngine(
            toolRegistry = ToolRegistry(),
            llmProvider = throwingProvider
        )

        val response = engine.executeVibecoding(
            promptText = "fail",
            workspaceRootPath = tempDir.toString(),
            maxActions = 2,
            sessionId = UUID.randomUUID(),
            agentContext = AgentContext(),
            model = null
        )

        assertEquals(SessionStatus.ERROR, response.status)
    }

    @Test
    fun `executeVibecoding records tool calls from registry audit entries`(@TempDir tempDir: Path) {
        val registry = ToolRegistry()
        val engine = SessionProtocolEngine(
            toolRegistry = registry,
            llmProvider = FakeLlmProvider()
        )

        val response = engine.executeVibecoding(
            promptText = "record tool calls",
            workspaceRootPath = tempDir.toString(),
            maxActions = 2,
            sessionId = UUID.randomUUID(),
            agentContext = AgentContext(),
            model = null
        )

        assertNotNull(response.toolCalls, "toolCalls should not be null")
    }

    @Test
    fun `executeVibecoding builds output containing intention classification iterations`(@TempDir tempDir: Path) {
        val engine = SessionProtocolEngine(
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )

        val response = engine.executeVibecoding(
            promptText = "Test output format",
            workspaceRootPath = tempDir.toString(),
            maxActions = 2,
            sessionId = UUID.randomUUID(),
            agentContext = AgentContext(),
            model = null
        )

        assertTrue(response.output.contains("=== Session Result ==="))
        assertTrue(response.output.contains("Intention: Test output format"))
        assertTrue(response.output.contains("Iterations:"))
        assertTrue(response.output.contains("Finished:"))
    }

    @Test
    fun `resolveAgentContext returns provided context when non-null`(@TempDir tempDir: Path) {
        val engine = SessionProtocolEngine(toolRegistry = ToolRegistry())
        val provided = AgentContext(eagerRules = "Rule 1")

        val resolved = engine.resolveAgentContext(provided, tempDir.toString())

        assertEquals(provided, resolved)
    }

    @Test
    fun `resolveAgentContext returns fallback AgentContext when governance missing`(@TempDir tempDir: Path) {
        val engine = SessionProtocolEngine(toolRegistry = ToolRegistry())

        val resolved = engine.resolveAgentContext(null, tempDir.toString())

        assertNotNull(resolved)
        assertTrue(resolved.eagerRules.isEmpty())
    }

    @Test
    fun `buildOutput contains error line when state has error`() {
        val engine = SessionProtocolEngine(toolRegistry = ToolRegistry())
        val state = codebase.koog.state.VibecodingState(
            intention = "errored task",
            workspaceRoot = "/tmp",
            maxActions = 1
        ).withError("something failed")

        val output = engine.buildOutput(state)

        assertTrue(output.contains("Error: something failed"))
        assertTrue(output.contains("Intention: errored task"))
    }

    @Test
    fun `buildOutput contains plan when planJson non-blank`() {
        val engine = SessionProtocolEngine(toolRegistry = ToolRegistry())
        val state = codebase.koog.state.VibecodingState(
            intention = "with plan",
            workspaceRoot = "/tmp",
            maxActions = 1,
            planJson = """{"title":"P","epics":[]}"""
        )

        val output = engine.buildOutput(state)

        assertTrue(output.contains("Plan: "))
    }

    @Test
    fun `buildOutput omits plan line when planJson blank`() {
        val engine = SessionProtocolEngine(toolRegistry = ToolRegistry())
        val state = codebase.koog.state.VibecodingState(
            intention = "no plan",
            workspaceRoot = "/tmp",
            maxActions = 1,
            planJson = ""
        )

        val output = engine.buildOutput(state)

        assertFalse(output.contains("Plan:"))
    }
}