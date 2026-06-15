package codebase.scenarios

import codebase.koog.SessionProtocolServer
import codebase.koog.llm.FakeLlmProvider
import codebase.koog.llm.LlmProvider
import contracts.vibecoding.registry.ToolRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class SessionProtocolServerWorld {
    var server: SessionProtocolServer? = null
    var llmProvider: LlmProvider = FakeLlmProvider()
    var outputLines: List<String> = emptyList()
    var workspaceRoot: String = ""
    var toolRegistry: ToolRegistry = ToolRegistry()

    fun configureAndRun(inputLines: List<String>) {
        if (workspaceRoot.isBlank()) {
            workspaceRoot = Files.createTempDirectory("sp-server-cucumber").toString()
        }

        val stdin = ByteArrayInputStream(inputLines.joinToString("\n").toByteArray(StandardCharsets.UTF_8))
        val stdout = ByteArrayOutputStream()

        server = SessionProtocolServer(
            workspaceRoot = workspaceRoot,
            toolRegistry = toolRegistry,
            llmProvider = llmProvider
        )
        server!!.run(stdin, stdout)

        outputLines = stdout.toString(StandardCharsets.UTF_8)
            .lines()
            .filter { it.isNotBlank() }
            .toList()
    }
}
