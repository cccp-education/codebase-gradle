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
    }

    @TaskAction
    fun executeProtocol() {
        val promptText = prompt.getOrElse("")
        if (promptText.isBlank()) {
            throw IllegalArgumentException("prompt cannot be blank — use -Pprompt=\"...\" or configure sessionProtocol { prompt = \"...\" }")
        }

        val sid = sessionId.getOrElse("").let {
            if (it.isNotBlank()) UUID.fromString(it) else UUID.randomUUID()
        }

        val agentContext = if (contextFile.isPresent) {
            parseContextFile(contextFile.get().asFile)
        } else null

        val sessionPrompt = SessionPrompt(
            sessionId = sid,
            prompt = promptText,
            context = agentContext,
            maxActions = maxActions.get(),
            model = model.getOrElse("").takeUnless { it.isBlank() }
        )

        log.info("[SessionProtocol] Starting session {} — prompt={}", sid, promptText)

        val tokenTracker = TokenTracker()
        val effectiveLlmProvider = llmProvider

        val graph = VibecodingGraph(
            augmentedGraph = KoogAugmentedContextGraph(),
            toolRegistry = toolRegistry,
            llmProvider = effectiveLlmProvider,
            tokenTracker = tokenTracker
        )

        val state = VibecodingState(
            intention = promptText,
            workspaceRoot = workspaceRoot.asFile.get().absolutePath,
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

        val response = SessionResponse(
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

        val outputFile = if (responseFile.isPresent) {
            responseFile.get().asFile
        } else {
            val dir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            dir.mkdirs()
            File(dir, "session-response.json")
        }

        val json = buildResponseJson(response)
        outputFile.writeText(json, Charsets.UTF_8)
        log.info("[SessionProtocol] Response written to {}", outputFile.absolutePath)

        if (result.error != null) {
            throw RuntimeException("Session protocol failed: ${result.error}")
        }
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
