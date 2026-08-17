package codebase.koog.discovery

import contracts.vibecoding.registry.ToolInfo
import contracts.vibecoding.registry.ToolRegistry

private val TASK_ALLOWLIST = listOf(
    Regex("^(build|compileKotlin|compileTestKotlin|test|testFast|check|assemble|jar|publishToMavenLocal|tasks|help)$"),
    Regex("^:?[\\w-]+:[\\w-]+$")
)

private val TASK_DENYLIST = listOf(
    Regex("publishToMavenCentral", RegexOption.IGNORE_CASE),
    Regex("publishAggregation", RegexOption.IGNORE_CASE),
    Regex("^clean$", RegexOption.IGNORE_CASE),
    Regex("^purge$", RegexOption.IGNORE_CASE),
    Regex("^wrapper$", RegexOption.IGNORE_CASE),
    Regex("^bootstrap$", RegexOption.IGNORE_CASE)
)

class TaskDiscoveryRegistrar(
    private val scanner: TaskSchemaScanner,
    private val registry: ToolRegistry
) {
    fun registerAll() {
        val schemas = scanner.scanAll()
        for (schema in schemas) {
            try {
                validateTaskName(schema.name)
            } catch (_: SecurityException) {
                continue
            }
            val toolInfo = schema.toToolInfo()
            try {
                registry.register(toolInfo)
            } catch (_: Exception) {
            }
        }
    }

    fun registeredCount(): Int {
        val schemas = scanner.scanAll()
        return schemas.count { schema ->
            try {
                validateTaskName(schema.name)
            } catch (_: SecurityException) {
                return@count false
            }
            val toolName = "gradle_${schema.name}"
            try {
                registry.get(toolName)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    companion object {
        fun validateTaskName(taskName: String) {
            val allowed = TASK_ALLOWLIST.any { it.matches(taskName) }
            if (!allowed) {
                throw SecurityException(
                    "Gradle task denied: not in allowlist (deny-by-default)"
                )
            }
            for (pattern in TASK_DENYLIST) {
                if (pattern.containsMatchIn(taskName)) {
                    throw SecurityException(
                        "Gradle task denied: matches denied pattern '${pattern.pattern}'"
                    )
                }
            }
        }
    }
}