package codebase.scenarios

import codebase.koog.SessionProtocolTask
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.session.AgentContext
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class GovernanceFallbackWorld {
    var projectDir: File? = null
    var responseFile: File? = null
    var responseContent: String = ""
    var thrownException: RuntimeException? = null
    var llmProvider: LlmProvider = FakeLlmProvider()
    var toolRegistry: ToolRegistry = ToolRegistry()
    var agentContext: AgentContext? = null

    fun newWorkspace(): File {
        val dir = Files.createTempDirectory("v-local-cucumber").toFile()
        projectDir = dir
        return dir
    }

    fun writeGovernanceFile(relativePath: String, content: String) {
        val file = File(projectDir!!, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    fun executeProtocol(prompt: String) {
        val dir = projectDir ?: Files.createTempDirectory("v-local-cucumber").toFile().also { projectDir = it }
        val project = ProjectBuilder.builder()
            .withProjectDir(dir)
            .withName("v-local-test")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set(prompt)
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = llmProvider
        task.toolRegistry = toolRegistry

        try {
            task.executeProtocol()
            agentContext = task.lastAgentContext
            val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            responseFile = outputDir.resolve("session-response.json")
            responseContent = responseFile?.readText(Charsets.UTF_8) ?: ""
        } catch (e: RuntimeException) {
            thrownException = e
            agentContext = task.lastAgentContext
            val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
            val f = outputDir.resolve("session-response.json")
            if (f.exists()) {
                responseFile = f
                responseContent = f.readText(Charsets.UTF_8)
            }
        }
    }
}