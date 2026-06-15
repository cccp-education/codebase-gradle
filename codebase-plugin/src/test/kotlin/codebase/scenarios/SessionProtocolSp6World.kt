package codebase.scenarios

import codebase.koog.LiveContextInjector
import codebase.koog.SessionProtocolLifecycleManager
import codebase.koog.SessionProtocolTask
import codebase.koog.llm.FakeLlmProvider
import contracts.session.AgentContext
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class SessionProtocolSp6World {
    var lifecycleDir: File = Files.createTempDirectory("sp6-lifecycle").toFile()
    var lifecycleManager: SessionProtocolLifecycleManager = SessionProtocolLifecycleManager(lifecycleDir)
    var responseContent: String = ""
    var llmPromptReceived: String = ""
    var contextFile: File? = null
    var thrownException: RuntimeException? = null

    fun executeAction(
        action: String,
        prompt: String = "",
        sessionId: String = "",
        maxActions: Int = 3,
        model: String = "",
        useContextFile: Boolean = false
    ) {
        val projectDir = Files.createTempDirectory("sp6-cucumber").toFile()
        val customResponseFile = Files.createTempFile("sp6-response", ".json").toFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName("sp6-test")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.action.set(action)
            it.maxActions.set(maxActions)
            it.responseFile.set(customResponseFile)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            if (prompt.isNotBlank()) it.prompt.set(prompt)
            if (sessionId.isNotBlank()) it.sessionId.set(sessionId)
            if (model.isNotBlank()) it.model.set(model)
            if (useContextFile && contextFile != null) {
                it.contextFile.set(project.layout.projectDirectory.file(contextFile!!.name))
                contextFile!!.copyTo(File(projectDir, contextFile!!.name), overwrite = true)
            }
        }.get()

        task.toolRegistry = ToolRegistry()
        task.lifecycleManager = lifecycleManager
        task.liveContextInjector = LiveContextInjector()

        val capturingProvider = object : codebase.koog.llm.LlmProvider {
            override suspend fun call(prompt: String): String {
                llmPromptReceived = prompt
                return FakeLlmProvider().call(prompt)
            }
        }
        task.llmProvider = capturingProvider

        try {
            task.executeProtocol()
            responseContent = customResponseFile.readText(Charsets.UTF_8)
        } catch (e: RuntimeException) {
            thrownException = e
            if (customResponseFile.exists()) {
                responseContent = customResponseFile.readText(Charsets.UTF_8)
            }
        }
    }

    fun createContextFile(eagerRules: String, backlogItems: List<String>) {
        val file = Files.createTempFile("sp6-context", ".json").toFile()
        val context = AgentContext(
            eagerRules = eagerRules,
            ragChunks = emptyList(),
            graphRelations = "",
            backlogItems = backlogItems
        )
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        file.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context))
        contextFile = file
    }
}
