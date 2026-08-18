package codebase.koog

import codebase.koog.llm.LlmProvider
import contracts.session.SessionPrompt
import contracts.session.SessionResponse
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import codebase.koog.governance.GovernanceContextLoader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
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
    private val liveContextInjector: LiveContextInjector? = null,
    var lastAgentContext: contracts.session.AgentContext? = null
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
        val agentContext = resolveAgentContext(sessionPrompt)
        val engine = SessionProtocolEngine(
            toolRegistry = toolRegistry,
            llmProvider = llmProvider,
            eventStream = eventStream,
            liveContextInjector = liveContextInjector
        )
        return engine.executeVibecoding(
            promptText = sessionPrompt.prompt,
            workspaceRootPath = workspaceRoot,
            maxActions = sessionPrompt.maxActions,
            sessionId = sessionPrompt.sessionId,
            agentContext = agentContext,
            model = sessionPrompt.model
        )
    }

    private fun resolveAgentContext(sessionPrompt: SessionPrompt): contracts.session.AgentContext {
        return sessionPrompt.context ?: try {
            log.info("[SessionProtocolServer] No context provided — auto-loading governance from {}", workspaceRoot)
            val root = File(workspaceRoot)
            val ctx = GovernanceContextLoader(graphFile = root.resolve("office/graph.json")).load(root)
            lastAgentContext = ctx
            ctx
        } catch (e: Exception) {
            log.warn("[SessionProtocolServer] Failed to auto-load governance context: {} — continuing without context", e.message)
            contracts.session.AgentContext()
        }
    }
}
