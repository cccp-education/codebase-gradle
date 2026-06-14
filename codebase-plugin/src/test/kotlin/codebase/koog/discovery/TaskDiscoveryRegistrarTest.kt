package codebase.koog.discovery

import codebase.CodebasePlugin
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskDiscoveryRegistrarTest {

    @Test
    fun `registerAll should register all discovered tasks as gradle_ tools`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()
        val initialCount = registry.toolCount()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val schemas = scanner.scanAll()
        assertTrue(registry.toolCount() > initialCount)
        schemas.forEach { schema ->
            val toolName = "gradle_${schema.name}"
            val tool = registry.get(toolName)
            assertNotNull(tool)
            assertEquals(toolName, tool.name)
        }
    }

    @Test
    fun `registeredCount should match number of discovered tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val schemas = scanner.scanAll()
        assertEquals(schemas.size, registrar.registeredCount())
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
        assertEquals(schemas.size, registrar.registeredCount())
    }

    @Test
    fun `registered tools should have gradle_ prefix`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val toolNames = registry.toolNames()
        val gradleTools = toolNames.filter { it.startsWith("gradle_") }
        assertTrue(gradleTools.isNotEmpty())
        assertTrue(gradleTools.contains("gradle_vibecode"))
        assertTrue(gradleTools.contains("gradle_qualityGate"))
    }

    @Test
    fun `registered tools should have descriptions with options`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val registry = ToolRegistry()

        val registrar = TaskDiscoveryRegistrar(scanner, registry)
        registrar.registerAll()

        val vibecodeTool = registry.get("gradle_vibecode")
        assertTrue(vibecodeTool.description.contains("Vibecoding"))
        assertTrue(vibecodeTool.description.contains("--intention") || vibecodeTool.description.contains("intention"))
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
