package codebase.koog.discovery

import codebase.CodebasePlugin
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskDiscoveryRegistrarTest {

    @Test
    fun `registerAll should skip non-allowlisted tasks deny-by-default`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()
        val initialCount = registry.toolCount()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val schemas = scanner.scanAll()
        val allowlistedSchemas = schemas.filter { schema ->
            runCatching { TaskDiscoveryRegistrar.validateTaskName(schema.name) }.isSuccess
        }
        val gradleTools = registry.toolNames().filter { it.startsWith("gradle_") }
        assertEquals(allowlistedSchemas.size, gradleTools.size)
        if (allowlistedSchemas.isNotEmpty()) {
            assertTrue(registry.toolCount() > initialCount)
        }
        allowlistedSchemas.forEach { schema ->
            val toolName = "gradle_${schema.name}"
            val tool = registry.get(toolName)
            assertNotNull(tool)
            assertEquals(toolName, tool.name)
        }
    }

    @Test
    fun `registeredCount should match number of allowlisted discovered tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val schemas = scanner.scanAll()
        val allowlisted = schemas.count { schema ->
            runCatching { TaskDiscoveryRegistrar.validateTaskName(schema.name) }.isSuccess
        }
        assertEquals(allowlisted, registrar.registeredCount())
    }

    @Test
    fun `registerAll should not crash on duplicate registration`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()
        registrar.registerAll()

        val schemas = scanner.scanAll()
        val allowlisted = schemas.count { schema ->
            runCatching { TaskDiscoveryRegistrar.validateTaskName(schema.name) }.isSuccess
        }
        assertEquals(allowlisted, registrar.registeredCount())
    }

    @Test
    fun `registered tools should have gradle_ prefix only for allowlisted tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val gradleTools = registry.toolNames().filter { it.startsWith("gradle_") }
        gradleTools.forEach { toolName ->
            val taskName = toolName.removePrefix("gradle_")
            runCatching { TaskDiscoveryRegistrar.validateTaskName(taskName) }
                .onFailure { org.junit.jupiter.api.fail("$toolName registered but $taskName not allowlisted") }
        }
    }

    @Test
    fun `non-allowlisted codebase tasks vibecode and qualityGate should be denied`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        assertNull(registry.toolNames().find { it == "gradle_vibecode" })
        assertNull(registry.toolNames().find { it == "gradle_qualityGate" })
    }

    @Test
    fun `registerAll should preserve existing N0 tools`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        assertNotNull(registry.get("read_file"))
        assertNotNull(registry.get("write_file"))
        assertNotNull(registry.get("edit_file"))
        assertNotNull(registry.get("list_directory"))
        assertNotNull(registry.get("exit"))
        assertNotNull(registry.get("exec_shell"))
        assertNotNull(registry.get("exec_gradle"))
    }
}
