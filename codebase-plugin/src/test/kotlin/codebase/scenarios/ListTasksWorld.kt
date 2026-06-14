package codebase.scenarios

import codebase.koog.discovery.TaskListFormatter
import codebase.koog.discovery.TaskOption
import codebase.koog.discovery.TaskSchema
import contracts.vibecoding.registry.ToolInfo
import contracts.vibecoding.registry.ToolRegistry

class ListTasksWorld {

    val schemas = listOf(
        TaskSchema("build", "Compiles the project", "build", "DefaultTask", emptyList()),
        TaskSchema("test", "Runs unit tests", "verification", "Test", listOf(
            TaskOption("tests", "Filter tests by name", false, "String")
        )),
        TaskSchema("publish", "Publishes artifacts to Maven Central", "publishing", "PublishToMavenRepository", listOf(
            TaskOption("repository", "Target repository name", false, "String"),
            TaskOption("dryRun", "Simulate without publishing", false, "Boolean")
        )),
        TaskSchema("clean", "Deletes build output", "build", "Delete", emptyList())
    )

    val toolRegistry: ToolRegistry = ToolRegistry().apply {
        register(ToolInfo("list_tasks",
            "List available Gradle tasks with descriptions and options"))
        registerHandler("list_tasks") { _, arguments, _ ->
            val group = arguments["group"].takeUnless { it.isNullOrBlank() }
            val keyword = arguments["keyword"].takeUnless { it.isNullOrBlank() }
            TaskListFormatter.format(schemas, group = group, keyword = keyword)
        }
    }

    var lastResult: String = ""
    var lastDryRun: Boolean = false

    fun reset() {
        lastResult = ""
        lastDryRun = false
    }
}
