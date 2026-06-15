package codebase.koog

import codebase.koog.llm.LlmProvider
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.slf4j.LoggerFactory

@DisableCachingByDefault(because = "Session protocol daemon — stdin/stdout interactive, non-cacheable")
abstract class SessionProtocolDaemonTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(SessionProtocolDaemonTask::class.java)

    @get:Internal
    var llmProvider: LlmProvider? = null

    @get:Internal
    var toolRegistry: ToolRegistry = ToolRegistry()

    init {
        group = "generate"
        description = "Session protocol daemon — stdin JSON-lines SessionPrompt → stdout SessionResponse, reuses Gradle daemon"
    }

    @TaskAction
    fun executeDaemon() {
        log.info("[SessionProtocolDaemon] Starting server on stdin/stdout...")
        val server = SessionProtocolServer(
            workspaceRoot = project.rootDir.absolutePath,
            toolRegistry = toolRegistry,
            llmProvider = llmProvider
        )
        server.run(System.`in`, System.out)
    }
}
