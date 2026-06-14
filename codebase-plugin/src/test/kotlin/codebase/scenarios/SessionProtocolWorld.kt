package codebase.scenarios

import codebase.koog.SessionProtocolTask
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.util.UUID

class SessionProtocolWorld {
    var projectDir: File? = null
    var responseFile: File? = null
    var responseContent: String = ""
    var thrownException: RuntimeException? = null
    var contextFile: File? = null
    var llmProvider: LlmProvider = FakeLlmProvider()
    var toolRegistry: ToolRegistry = ToolRegistry()

    fun executeProtocol(
        prompt: String,
        sessionId: String = "",
        maxActions: Int = 3,
        model: String = "",
        useContextFile: Boolean = false
    ) {
        val dir = Files.createTempDirectory("sp-cucumber").toFile()
        projectDir = dir

        val project = ProjectBuilder.builder()
            .withProjectDir(dir)
            .withName("sp-test")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set(prompt)
            it.maxActions.set(maxActions)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            if (sessionId.isNotBlank()) it.sessionId.set(sessionId)
            if (model.isNotBlank()) it.model.set(model)
            if (useContextFile && contextFile != null) it.contextFile.set(contextFile!!)
        }.get()

        task.llmProvider = llmProvider
        task.toolRegistry = toolRegistry

        try {
            task.executeProtocol()
            val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            responseFile = outputDir.resolve("session-response.json")
            responseContent = responseFile!!.readText(Charsets.UTF_8)
        } catch (e: RuntimeException) {
            thrownException = e
            val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            val f = outputDir.resolve("session-response.json")
            if (f.exists()) {
                responseFile = f
                responseContent = f.readText(Charsets.UTF_8)
            }
        }
    }
}
