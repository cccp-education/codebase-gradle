package codebase.koog

import codebase.koog.llm.LlmProvider
import codebase.koog.tracking.TokenTracker
import contracts.session.AgentContext
import contracts.session.SessionPrompt
import contracts.session.SessionResponse
import contracts.session.SessionStatus
import contracts.session.TokenUsage
import contracts.session.ToolCallRecord
import contracts.vibecoding.registry.ToolRegistry
import vibecoding.contracts.state.VibecodingState
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.UUID

@DisableCachingByDefault(because = "Session protocol — LLM calls non-deterministic, non-cacheable")
abstract class SessionProtocolTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(SessionProtocolTask::class.java)

    @get:Internal
    var llmProvider: LlmProvider? = null

    @get:Internal
    var toolRegistry: ToolRegistry = ToolRegistry()

    @get:Internal
    var lifecycleManager: SessionProtocolLifecycleManager? = null

    @get:Input
    @get:Optional
    @get:Option(option = "prompt", description = "Prompt utilisateur à exécuter")
    abstract val prompt: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "sessionId", description = "ID de session (UUID). Généré automatiquement si absent.")
    abstract val sessionId: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "maxActions", description = "Nombre maximum d'actions (défaut 10)")
    abstract val maxActions: Property<Int>

    @get:Input
    @get:Optional
    @get:Option(option = "model", description = "Modèle LLM à utiliser")
    abstract val model: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "action", description = "Action lifecycle: create, resume, close, list")
    abstract val action: Property<String>

    @get:InputFile
    @get:Optional
    @get:Option(option = "contextFile", description = "Fichier JSON AgentContext (optionnel)")
    abstract val contextFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    @get:Option(option = "responseFile", description = "Fichier de sortie SessionResponse JSON")
    abstract val responseFile: RegularFileProperty

    @get:Internal
    abstract val workspaceRoot: RegularFileProperty

    init {
        group = "generate"
        description = "Session protocol — reçoit SessionPrompt, exécute VibecodingGraph, retourne SessionResponse"
        prompt.convention("")
        sessionId.convention("")
        maxActions.convention(10)
        model.convention("")
        action.convention("create")
    }

    private fun resolveLifecycleManager(): SessionProtocolLifecycleManager {
        return lifecycleManager ?: SessionProtocolLifecycleManager(
            project.layout.buildDirectory.dir("session-protocol").get().asFile
        )
    }

    @TaskAction
    fun executeProtocol() {
        val actionValue = action.getOrElse("create")
        when (actionValue) {
            "create" -> executeCreate()
            "resume" -> executeResume()
            "close" -> executeClose()
            "list" -> executeList()
            else -> throw IllegalArgumentException("Unknown action: $actionValue. Valid: create, resume, close, list")
        }
    }

    private fun executeCreate() {
        val promptText = requirePrompt()
        val explicitSessionId = sessionId.getOrElse("").takeUnless { it.isBlank() }

        val lifecycleMgr = resolveLifecycleManager()
        val lifecycleState = lifecycleMgr.create(
            prompt = promptText,
            model = model.getOrElse("").takeIf { it.isNotBlank() },
            sessionId = explicitSessionId
        )

        val response = executeVibecoding(promptText, UUID.fromString(lifecycleState.sessionId))

        val json = buildResponseJson(response)
        lifecycleMgr.updateResponse(lifecycleState.sessionId, json)

        val outputFile = resolveOutputFile()
        outputFile.writeText(json, Charsets.UTF_8)
        log.info("[SessionProtocol] Created session {} — response -> {}", lifecycleState.sessionId, outputFile.absolutePath)

        if (response.status == SessionStatus.ERROR) {
            throw RuntimeException("Session protocol failed: ${response.output}")
        }
    }

    private fun executeResume() {
        val promptText = requirePrompt()
        val parentId = requireSessionId()

        val lifecycleMgr = resolveLifecycleManager()
        val childState = lifecycleMgr.resume(parentId)

        val response = executeVibecoding(promptText, UUID.fromString(childState.sessionId))

        val json = buildResponseJson(response)
        lifecycleMgr.updateResponse(childState.sessionId, json)

        val outputFile = resolveOutputFile()
        outputFile.writeText(json, Charsets.UTF_8)
        log.info("[SessionProtocol] Resumed session {} -> child {} -> response -> {}", parentId, childState.sessionId, outputFile.absolutePath)

        if (response.status == SessionStatus.ERROR) {
            throw RuntimeException("Session protocol failed: ${response.output}")
        }
    }

    private fun executeClose() {
        val sid = requireSessionId()
        val lifecycleMgr = resolveLifecycleManager()
        lifecycleMgr.close(sid)
        log.info("[SessionProtocol] Closed session {}", sid)
    }

    private fun executeList() {
        val lifecycleMgr = resolveLifecycleManager()
        val sessions = lifecycleMgr.list()
        val outputFile = resolveOutputFile()
        val json = buildResponseJson(buildListResponse(sessions))
        outputFile.writeText(json, Charsets.UTF_8)
        log.info("[SessionProtocol] Listed {} sessions -> {}", sessions.size, outputFile.absolutePath)
    }

    private fun executeVibecoding(promptText: String, sid: UUID): SessionResponse {
        val agentContext = if (contextFile.isPresent) {
            parseContextFile(contextFile.get().asFile)
        } else null

        log.info("[SessionProtocol] Executing session {} — prompt={}", sid, promptText)

        val tokenTracker = TokenTracker()

        val graph = VibecodingGraph(
            augmentedGraph = KoogAugmentedContextGraph(),
            toolRegistry = toolRegistry,
            llmProvider = llmProvider,
            tokenTracker = tokenTracker
        )

        val state = VibecodingState(
            intention = promptText,
            workspaceRoot = workspaceRoot.asFile.get().absolutePath,
            dryRun = false,
            maxActions = maxActions.get()
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
            sessionId = sid,
            output = buildOutput(result),
            toolCalls = toolCalls,
            tokenUsage = TokenUsage(
                promptTokens = tokenTracker.promptTokens.toInt(),
                completionTokens = tokenTracker.completionTokens.toInt(),
                totalTokens = (tokenTracker.promptTokens + tokenTracker.completionTokens).toInt(),
                cost = tokenTracker.estimatedCost(model.getOrElse("unknown"))
            ),
            status = status
        )
    }

    private fun requirePrompt(): String {
        val text = prompt.getOrElse("")
        if (text.isBlank()) {
            throw IllegalArgumentException("prompt cannot be blank — use -Pprompt=\"...\" or --prompt=\"...\"")
        }
        return text
    }

    private fun requireSessionId(): String {
        val sid = sessionId.getOrElse("")
        if (sid.isBlank()) {
            throw IllegalArgumentException("sessionId is required for this action — use -PsessionId=\"...\" or --sessionId=\"...\"")
        }
        return sid
    }

    private fun resolveOutputFile(): File {
        return if (responseFile.isPresent) {
            responseFile.get().asFile
        } else {
            val dir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            dir.mkdirs()
            File(dir, "session-response.json")
        }
    }

    private fun buildListResponse(sessions: List<SessionLifecycleState>): SessionResponse {
        val lines = sessions.map { s ->
            "[${s.status.name}] ${s.sessionId.take(8)}... | ${s.prompt.take(60)} | model=${s.model ?: "default"} | created=${s.createdAt}"
        }
        return SessionResponse(
            sessionId = UUID.randomUUID(),
            output = if (lines.isEmpty()) "No sessions found." else lines.joinToString("\n"),
            status = SessionStatus.COMPLETED
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

    private fun parseContextFile(file: File): AgentContext {
        val content = file.readText(Charsets.UTF_8)
        return try {
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            mapper.readValue(content, AgentContext::class.java)
        } catch (e: Exception) {
            log.warn("[SessionProtocol] Failed to parse context file: {}", e.message)
            AgentContext()
        }
    }

    private fun buildResponseJson(response: SessionResponse): String {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response)
    }
}
