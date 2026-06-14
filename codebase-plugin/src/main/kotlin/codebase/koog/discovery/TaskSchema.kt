package codebase.koog.discovery

import contracts.vibecoding.registry.ToolInfo

data class TaskSchema(
    val name: String,
    val description: String,
    val group: String,
    val type: String,
    val options: List<TaskOption>
) {
    fun toToolInfo(): ToolInfo = ToolInfo(
        name = "gradle_$name",
        description = buildString {
            append(description)
            if (options.isNotEmpty()) {
                append(" Options: ")
                append(options.joinToString(", ") { "--${it.name}" })
            }
        }
    )
}

data class TaskOption(
    val name: String,
    val description: String,
    val required: Boolean,
    val type: String
)
