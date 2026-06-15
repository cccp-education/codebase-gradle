package codebase.scenarios

import codebase.koog.SessionProtocolLifecycleManager
import codebase.koog.SessionProtocolServer
import codebase.koog.SessionProtocolTask
import codebase.koog.ToolEventStream
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.vibecoding.registry.ToolRegistry
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.testfixtures.ProjectBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class SessionProtocolSp5World {
    var lifecycleDir: File = Files.createTempDirectory("sp5-lifecycle").toFile()
    var lifecycleManager: SessionProtocolLifecycleManager = SessionProtocolLifecycleManager(lifecycleDir)
    var eventOutput = ByteArrayOutputStream()
    var eventStream: ToolEventStream = ToolEventStream(eventOutput)
    var responseContent: String = ""
    var thrownException: RuntimeException? = null
    var serverOutput = ByteArrayOutputStream()
    var serverEventStream: ToolEventStream = ToolEventStream(serverOutput)
    var useFailingProvider: Boolean = false

    fun executeAction(
        action: String,
        prompt: String = "",
        sessionId: String = "",
        maxActions: Int = 3,
        model: String = "",
        failingProvider: Boolean = false
    ) {
        val projectDir = Files.createTempDirectory("sp5-cucumber").toFile()
        val customResponseFile = Files.createTempFile("sp5-response", ".json").toFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(projectDir)
            .withName("sp5-test")
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
        }.get()

        task.toolRegistry = ToolRegistry()
        task.lifecycleManager = lifecycleManager
        task.eventStream = eventStream

        if (useFailingProvider) {
            task.llmProvider = object : LlmProvider {
                override suspend fun call(prompt: String): String {
                    throw RuntimeException("Simulated LLM failure")
                }
            }
        } else {
            task.llmProvider = FakeLlmProvider()
        }

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

    fun runServer(prompts: List<String>) {
        val input = prompts.joinToString("\n") + "\n\n"
        val stdin = ByteArrayInputStream(input.toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        val server = SessionProtocolServer(
            workspaceRoot = Files.createTempDirectory("sp5-server").toString(),
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider(),
            eventStream = serverEventStream
        )
        server.run(stdin, stdout)
    }

    fun eventLines(): List<Map<String, Any>> {
        val mapper = jacksonObjectMapper()
        return eventOutput.toString(StandardCharsets.UTF_8).trim().lines()
            .filter { it.isNotBlank() }
            .map { mapper.readValue<Map<String, Any>>(it) }
    }

    fun serverEventLines(): List<Map<String, Any>> {
        val mapper = jacksonObjectMapper()
        return serverOutput.toString(StandardCharsets.UTF_8).trim().lines()
            .filter { it.isNotBlank() }
            .map { mapper.readValue<Map<String, Any>>(it) }
    }
}
