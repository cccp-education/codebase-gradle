package codebase.koog.discovery

class TaskListFormatter {

    companion object {
        fun format(schemas: List<TaskSchema>, group: String? = null, keyword: String? = null): String {
            val filtered = schemas
                .filter { schema -> group == null || schema.group.equals(group, ignoreCase = true) }
                .filter { schema ->
                    if (keyword == null) return@filter true
                    val kw = keyword.lowercase()
                    schema.name.lowercase().contains(kw) || schema.description.lowercase().contains(kw)
                }

            if (filtered.isEmpty()) return "No tasks found."

            return filtered.joinToString("\n---\n") { schema ->
                buildString {
                    append("- gradle_${schema.name}\n")
                    append("  group: ${schema.group}\n")
                    append("  description: ${schema.description}\n")
                    if (schema.options.isNotEmpty()) {
                        append("  options:\n")
                        schema.options.forEach { opt ->
                            append("    --${opt.name}: ${opt.type}")
                            if (opt.description.isNotBlank()) append(" — ${opt.description}")
                            append("\n")
                        }
                    } else {
                        append("  options: none\n")
                    }
                }
            }
        }
    }
}
