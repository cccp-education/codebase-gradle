package codebase.koog.discovery

import codebase.CodebasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskSchemaScannerTest {

    @Test
    fun `scanAll discovers all CodebasePlugin tasks`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val schemas = scanner.scanAll()
        val pluginTaskNames = setOf(
            "collectFromCodebase", "collectCompositeContext", "ocrDocument", "ocrIngest",
            "generatePlan", "vibecode", "sessionProtocolDaemon", "ingestGovernance",
            "vibecodingDashboard", "qualityGate", "endSessionBlog",
            "generateCompositeContext", "exposeExperts"
        )

        val discoveredNames = schemas.map { it.name }.toSet()
        assertTrue(pluginTaskNames.all { it in discoveredNames },
            "Missing plugin tasks: ${pluginTaskNames - discoveredNames}")
    }

    @Test
    fun `scanAll captures task name and group`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val vibecode = scanner.scanAll().find { it.name == "vibecode" }

        assertNotNull(vibecode)
        assertEquals("generate", vibecode.group)
    }

    @Test
    fun `vibecode task has options extracted`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val vibecode = scanner.scanAll().find { it.name == "vibecode" }

        assertNotNull(vibecode)
        val optionNames = vibecode.options.map { it.name }.toSet()
        assertTrue(optionNames.contains("intention"))
        assertTrue(optionNames.contains("dryRun"))
        assertTrue(optionNames.contains("maxActions"))
        assertTrue(optionNames.contains("model"))
    }

    @Test
    fun `qualityGate task has options extracted`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val qualityGate = scanner.scanAll().find { it.name == "qualityGate" }

        assertNotNull(qualityGate)
        assertEquals("validate", qualityGate.group)
        val optionNames = qualityGate.options.map { it.name }.toSet()
        assertTrue(optionNames.contains("output"))
        assertTrue(optionNames.contains("domain"))
        assertTrue(optionNames.contains("minAcceptableScore"))
    }

    @Test
    fun `scanByGroup filters by group correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val collectTasks = scanner.scanByGroup("collect")
        val generateTasks = scanner.scanByGroup("generate")

        assertEquals(4, collectTasks.size)
        assertEquals(7, generateTasks.size)
        assertTrue(collectTasks.all { it.group == "collect" })
        assertTrue(generateTasks.all { it.group == "generate" })
    }

    @Test
    fun `toToolInfo maps correct gradle_ prefix`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val vibecode = scanner.scanAll().find { it.name == "vibecode" }
        assertNotNull(vibecode)

        val toolInfo = vibecode.toToolInfo()
        assertEquals("gradle_vibecode", toolInfo.name)
        assertTrue(toolInfo.description.contains("Vibecoding agent"))
        assertTrue(toolInfo.description.contains("--intention") || toolInfo.description.contains("intention"))
    }

    @Test
    fun `tasks without group are excluded`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)

        val schemas = scanner.scanAll()

        assertTrue(schemas.all { it.group.isNotBlank() })
    }
}
