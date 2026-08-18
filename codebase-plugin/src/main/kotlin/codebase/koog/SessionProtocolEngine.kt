package codebase.koog

import codebase.koog.governance.GovernanceContextLoader
import codebase.koog.llm.LlmProvider
import codebase.koog.state.VibecodingState
import codebase.koog.tracking.TokenTracker
import contracts.session.AgentContext
import contracts.session.SessionResponse
import contracts.session.SessionStatus
import contracts.session.TokenUsage
import contracts.session.ToolCallRecord
import contracts.vibecoding.registry.ToolRegistry
import java.io.File

class SessionProtocolEngine(
    val toolRegistry: ToolRegistry,
    val llmProvider: LlmProvider? = null,
    val eventStream: ToolEventStream? = null,
    val liveContextInjector: LiveContextInjector? = null
) {
    fun executeVibecoding(
        promptText: String,
        workspaceRootPath: String,
        maxActions: Int,
        sessionId: java.util.UUID,
        agentContext: AgentContext,
        model: String? = null
    ): SessionResponse {
        val tokenTracker = TokenTracker()

        val graph = VibecodingGraph(
            augmentedGraph = KoogAugmentedContextGraph(),
            toolRegistry = toolRegistry,
            llmProvider = llmProvider,
            tokenTracker = tokenTracker,
            eventStream = eventStream,
            liveContextInjector = liveContextInjector
        )
        graph.staticContext = agentContext

        val state = VibecodingState(
            intention = promptText,
            workspaceRoot = workspaceRootPath,
            dryRun = false,
            maxActions = maxActions
        )

        val result = graph.execute(state)

        val toolCalls = toolRegistry.auditEntries().map { entry ->
            ToolCallRecord(
                toolName = entry.tool,
                args = emptyMap(),
                result = entry.result,
                timestamp = entry.timestamp
            )
        }

        val status = when {
            result.error != null -> SessionStatus.ERROR
            result.finished -> SessionStatus.COMPLETED
            else -> SessionStatus.IN_PROGRESS
        }

        return SessionResponse(
            sessionId = sessionId,
            output = buildOutput(result),
            toolCalls = toolCalls,
            tokenUsage = TokenUsage(
                promptTokens = tokenTracker.promptTokens.toInt(),
                completionTokens = tokenTracker.completionTokens.toInt(),
                totalTokens = (tokenTracker.promptTokens + tokenTracker.completionTokens).toInt(),
                cost = tokenTracker.estimatedCost(model ?: "unknown")
            ),
            status = status
        )
    }

    fun resolveAgentContext(
        context: AgentContext?,
        workspaceRootPath: String,
        onContextResolved: (AgentContext) -> Unit = {}
    ): AgentContext {
        return context ?: try {
            val root = File(workspaceRootPath)
            val ctx = GovernanceContextLoader(graphFile = root.resolve("office/graph.json")).load(root)
            onContextResolved(ctx)
            ctx
        } catch (e: Exception) {
            AgentContext()
        }
    }

    fun buildOutput(state: VibecodingState): String = buildString {
        appendLine("=== Session Result ===")
        appendLine("Intention: ${state.intention}")
        appendLine("Classification: ${state.classification}")
        appendLine("Iterations: ${state.iteration}")
        appendLine("Finished: ${state.finished}")
        if (state.error != null) {
            appendLine("Error: ${state.error}")
        }
        if (state.planJson.isNotBlank()) {
            appendLine("Plan: ${state.planJson}")
        }
        appendLine("Executed tasks: ${state.executedTasks.joinToString(", ")}")
    }
}