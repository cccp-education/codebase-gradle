package codebase.koog.discovery

import codebase.CodebasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiSpecGeneratorTest {

    private val generator = OpenApiSpecGenerator()

    @Test
    fun `generate should produce valid OpenAPI 3_0_0 spec`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        assertEquals("3.0.0", spec.openapi)
        assertNotNull(spec.info)
        assertEquals("Gradle Tasks API", spec.info.title)
        assertEquals("1.0.0", spec.info.version)
    }

    @Test
    fun `generate should create one path per task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        assertEquals(schemas.size, spec.paths.size)
        schemas.forEach { schema ->
            val path = "/tasks/${schema.name}"
            assertTrue(spec.paths.containsKey(path), "Missing path: $path")
        }
    }

    @Test
    fun `generate should include task options as query parameters`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        val vibecodePath = spec.paths["/tasks/vibecode"]
        assertNotNull(vibecodePath)
        val operation = vibecodePath.post
        assertNotNull(operation)
        assertEquals("execute_vibecode", operation.operationId)

        val paramNames = operation.parameters.map { it.name }.toSet()
        assertTrue(paramNames.contains("intention"))
        assertTrue(paramNames.contains("dryRun"))
        assertTrue(paramNames.contains("maxActions"))
        assertTrue(paramNames.contains("model"))
    }

    @Test
    fun `generate should map Kotlin types to OpenAPI types`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        val vibecodePath = spec.paths["/tasks/vibecode"]
        assertNotNull(vibecodePath)
        val operation = vibecodePath.post
        assertNotNull(operation)

        val dryRunParam = operation.parameters.find { it.name == "dryRun" }
        assertNotNull(dryRunParam)
        assertEquals("boolean", dryRunParam.schema.type)

        val maxActionsParam = operation.parameters.find { it.name == "maxActions" }
        assertNotNull(maxActionsParam)
        assertEquals("integer", maxActionsParam.schema.type)
    }

    @Test
    fun `generate should include group and type in operation description`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        val vibecodePath = spec.paths["/tasks/vibecode"]
        assertNotNull(vibecodePath)
        val operation = vibecodePath.post
        assertNotNull(operation)
        assertTrue(operation.description.contains("Group:"))
        assertTrue(operation.description.contains("generate"))
        assertTrue(operation.description.contains("Type:"))
    }

    @Test
    fun `generateJson should produce valid JSON string`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val json = generator.generateJson(schemas)

        assertTrue(json.startsWith("{"))
        assertTrue(json.contains("\"openapi\""))
        assertTrue(json.contains("\"3.0.0\""))
        assertTrue(json.contains("\"paths\""))
        assertTrue(json.contains("\"/tasks/vibecode\""))
    }

    @Test
    fun `generate should handle empty schema list`() {
        val spec = generator.generate(emptyList())

        assertEquals("3.0.0", spec.openapi)
        assertTrue(spec.paths.isEmpty())
    }

    @Test
    fun `generate should include 200 and 400 responses`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        val scanner = TaskSchemaScanner(project)
        val schemas = scanner.scanAll()

        val spec = generator.generate(schemas)

        val vibecodePath = spec.paths["/tasks/vibecode"]
        assertNotNull(vibecodePath)
        val operation = vibecodePath.post
        assertNotNull(operation)
        assertTrue(operation.responses.containsKey("200"))
        assertTrue(operation.responses.containsKey("400"))
    }
}
