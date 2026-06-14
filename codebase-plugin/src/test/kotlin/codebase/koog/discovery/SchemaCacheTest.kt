package codebase.koog.discovery

import codebase.CodebasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaCacheTest {

    private fun projectWithTasks() = ProjectBuilder.builder().build().also { project ->
        project.tasks.register("buildTask") { it.group = "build"; it.description = "Builds" }
        project.tasks.register("testTask") { it.group = "verification"; it.description = "Tests" }
    }

    @Test
    fun `first access should scan and cache schemas`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas = cache.schemas()
        assertTrue(schemas.isNotEmpty())
        assertEquals(2, schemas.size)
    }

    @Test
    fun `second access without change should return cached instance`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas1 = cache.schemas()
        val schemas2 = cache.schemas()
        assertSame(schemas1, schemas2, "Should return same cached instance")
    }

    @Test
    fun `adding a plugin should not invalidate without explicit call`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas1 = cache.schemas()
        val initialSize = schemas1.size

        project.pluginManager.apply(CodebasePlugin::class.java)
        cache.invalidate()
        val schemas2 = cache.schemas()

        assertTrue(schemas2.size > initialSize, "After explicit invalidate + plugin: $initialSize → ${schemas2.size}")
    }

    @Test
    fun `manual invalidate should force rescan`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas1 = cache.schemas()
        val cachedSize = schemas1.size

        project.tasks.register("dynamicTask") { it.group = "build" }
        cache.invalidate()
        val schemas2 = cache.schemas()

        assertEquals(cachedSize + 1, schemas2.size, "After invalidation + new task: expected ${cachedSize + 1}, got ${schemas2.size}")
    }

    @Test
    fun `adding task without invalidate should still serve cached`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas1 = cache.schemas()
        project.tasks.register("postCacheTask") { it.group = "build" }
        val schemas2 = cache.schemas()

        assertSame(schemas1, schemas2, "Without invalidation, should return same cache")
    }

    @Test
    fun `schemas from cache should contain task names`() {
        val project = projectWithTasks()
        val cache = SchemaCache(project)
        val schemas = cache.schemas()
        val names = schemas.map { it.name }.toSet()
        project.tasks.filter { it.group != null }.forEach { task ->
            assertTrue(names.contains(task.name), "Cache should contain task '${task.name}'")
        }
    }
}
