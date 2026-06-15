package codebase.scenarios

import codebase.koog.SessionProtocolLifecycleManager
import codebase.koog.SessionProtocolTask
import codebase.koog.llm.FakeLlmProvider
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class SessionProtocolE2EWorld {
    var lifecycleDir: File = Files.createTempDirectory("sp4-e2e").toFile()
    var lifecycleManager: SessionProtocolLifecycleManager = SessionProtocolLifecycleManager(lifecycleDir)
    var responseFile: File? = null
    var responseContent: String = ""
    var thrownException: RuntimeException? = null
    var contextFile: File? = null
    var createdSessionId: String? = null
    var childSessionId: String? = null

    fun setupLifecycleManager(dir: File) {
        lifecycleDir = dir
        lifecycleManager = SessionProtocolLifecycleManager(dir)
    }

    fun executeAction(
        action: String,
        prompt: String = "",
        sessionId: String = "",
        maxActions: Int = 3,
        model: String = "",
        useContextFile: Boolean = false
    ) {
        val projectDir = Files.createTempDirectory("sp4-e2e-cucumber").toFile()
        val customResponseFile = Files.createTempFile("sp4-e2e-response", ".json").toFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName("sp4-e2e-test")
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
            if (useContextFile && contextFile != null) it.contextFile.set(contextFile!!)
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()
        task.lifecycleManager = lifecycleManager

        try {
            task.executeProtocol()
            responseFile = customResponseFile
            responseContent = customResponseFile.readText(Charsets.UTF_8)
        } catch (e: RuntimeException) {
            thrownException = e
            if (customResponseFile.exists()) {
                responseFile = customResponseFile
                responseContent = customResponseFile.readText(Charsets.UTF_8)
            }
        }
    }
}
