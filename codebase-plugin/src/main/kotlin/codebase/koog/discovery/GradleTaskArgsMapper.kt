package codebase.koog.discovery

import contracts.vibecoding.tools.ExecGradleTool

class GradleTaskArgsMapper(
    private val schemas: List<TaskSchema>,
    private val workingDir: String? = null
) {
    fun buildCommand(toolName: String, arguments: Map<String, String>): String {
        val taskName = toolName.removePrefix("gradle_")
        val schema = schemas.find { it.name == taskName }
        val optionNames = schema?.options?.associate { it.name to it } ?: emptyMap()

        val flagPairs = arguments
            .filterKeys { optionNames.containsKey(it) }
            .map { (key, value) -> "--$key $value" }

        val flags = flagPairs.joinToString(" ")
        return if (flags.isEmpty()) taskName else "$taskName $flags"
    }

    fun execute(toolName: String, arguments: Map<String, String>, workspaceRoot: String): String {
        val cmd = buildCommand(toolName, arguments)
        val dir = workingDir ?: workspaceRoot
        return ExecGradleTool.executeBlocking(task = cmd, workingDir = dir)
    }
}
