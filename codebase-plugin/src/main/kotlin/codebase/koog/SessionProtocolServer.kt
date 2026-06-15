package codebase.koog

import codebase.koog.llm.LlmProvider
import codebase.koog.tracking.TokenTracker
import contracts.session.SessionPrompt
import contracts.session.SessionResponse
import contracts.session.SessionStatus
import contracts.session.TokenUsage
import contracts.session.ToolCallRecord
import contracts.vibecoding.registry.ToolRegistry
import vibecoding.contracts.state.VibecodingState
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.Scanner
import java.util.UUID

class SessionProtocolServer(
    private val workspaceRoot: String,
    private val toolRegistry: ToolRegistry = ToolRegistry(),
    private val llmProvider: LlmProvider? = null,
    private val eventStream: ToolEventStream? = null,
    private val liveContextInjector: LiveContextInjector? = null
) {
    private val log = LoggerFactory.getLogger(SessionProtocolServer::class.java)
    private val mapper = jacksonObjectMapper()

    fun run(stdin: InputStream, stdout: OutputStream) {
        val scanner = Scanner(stdin, Charsets.UTF_8)
        val writer = PrintWriter(stdout, true, Charsets.UTF_8)

        while (scanner.hasNextLine()) {
            val line = scanner.nextLine().trim()
            if (line.isBlank()) continue

            val sessionPrompt = try {
                mapper.readValue<SessionPrompt>(line)
            } catch (e: Exception) {
                log.warn("[SessionProtocolServer] Failed to parse prompt: {}", e.message)
                val errorResponse = SessionResponse(
                    sessionId = UUID.randomUUID(),
                    output = "Invalid JSON: ${e.message}",
                    status = SessionStatus.ERROR
                )
                writer.println(mapper.writeValueAsString(errorResponse))
                continue
            }

            if (sessionPrompt.prompt.isBlank()) {
                writer.flush()
                break
            }

            try {
                eventStream?.currentSessionId = sessionPrompt.sessionId.toString()
                val response = executePrompt(sessionPrompt)
                writer.println(mapper.writeValueAsString(response))
            } catch (e: Exception) {
                log.error("[SessionProtocolServer] Error executing prompt: {}", e.message)
                val errorResponse = SessionResponse(
                    sessionId = sessionPrompt.sessionId,
                    output = "Error: ${e.message}",
                    status = SessionStatus.ERROR
                )
                writer.println(mapper.writeValueAsString(errorResponse))
            }
            writer.flush()
        }

        writer.flush()
    }

    private fun executePrompt(sessionPrompt: SessionPrompt): SessionResponse {
        val tokenTracker = TokenTracker()

        val graph = VibecodingGraph(
            augmentedGraph = KoogAugmentedContextGraph(),
            toolRegistry = toolRegistry,
            llmProvider = llmProvider,
            tokenTracker = tokenTracker,
            eventStream = eventStream,
            liveContextInjector = liveContextInjector
        )
        graph.staticContext = sessionPrompt.context

        val state = VibecodingState(
            intention = sessionPrompt.prompt,
            workspaceRoot = workspaceRoot,
            dryRun = false,
            maxActions = sessionPrompt.maxActions
        )

        val result = graph.execute(state)

        val toolCalls = toolRegistry.auditEntries().map { entry ->
            ToolCallRecord(
                toolName = entry.tool,
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
            sessionId = sessionPrompt.sessionId,
            output = buildOutput(result),
            toolCalls = toolCalls,
            tokenUsage = TokenUsage(
                promptTokens = tokenTracker.promptTokens.toInt(),
                completionTokens = tokenTracker.completionTokens.toInt(),
                totalTokens = (tokenTracker.promptTokens + tokenTracker.completionTokens).toInt(),
                cost = tokenTracker.estimatedCost(sessionPrompt.model ?: "unknown")
            ),
            status = status
        )
    }

    private fun buildOutput(state: VibecodingState): String = buildString {
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
