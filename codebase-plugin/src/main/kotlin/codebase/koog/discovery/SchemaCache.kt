package codebase.koog.discovery

import org.gradle.api.Project

class SchemaCache(private val project: Project) {

    private var cached: List<TaskSchema>? = null
    private var dirty: Boolean = true

    fun schemas(): List<TaskSchema> {
        if (dirty || cached == null) {
            val scanner = TaskSchemaScanner(project)
            cached = scanner.scanAll()
            dirty = false
        }
        return cached!!
    }

    fun invalidate() {
        dirty = true
    }
}
