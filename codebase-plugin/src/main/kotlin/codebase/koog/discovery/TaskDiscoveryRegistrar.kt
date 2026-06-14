package codebase.koog.discovery

import contracts.vibecoding.registry.ToolInfo
import contracts.vibecoding.registry.ToolRegistry

class TaskDiscoveryRegistrar(
    private val scanner: TaskSchemaScanner,
    private val registry: ToolRegistry
) {
    fun registerAll() {
        val schemas = scanner.scanAll()
        for (schema in schemas) {
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
            val toolName = "gradle_${schema.name}"
            try {
                registry.get(toolName)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
