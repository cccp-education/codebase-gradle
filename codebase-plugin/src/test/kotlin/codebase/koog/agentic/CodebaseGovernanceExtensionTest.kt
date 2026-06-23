package codebase.koog.agentic

import codebase.CodebasePlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodebaseGovernanceExtensionTest {

    @Test
    fun `extension is registered by plugin with defaults`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply(CodebasePlugin::class.java)

        val ext = project.extensions.findByType(CodebaseGovernanceExtension::class.java)
        assertNotNull(ext, "codebaseGovernance extension should be registered")
        assertFalse(ext.strictValidation.getOrElse(true), "strictValidation should default to false")
        assertTrue(ext.outputEnabled.getOrElse(false), "outputEnabled should default to true")
        assertEquals("json", ext.reportFormat.getOrElse(""))
        assertFalse(ext.incremental.getOrElse(true), "incremental should default to false")
    }

    @Test
    fun `extension toConfig returns DDD model`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply(CodebasePlugin::class.java)

        val ext = project.extensions.getByType(CodebaseGovernanceExtension::class.java)
        ext.strictValidation.set(true)
        ext.outputEnabled.set(false)
        ext.reportFormat.set("json")
        ext.incremental.set(true)

        val config = ext.toConfig()
        assertTrue(config.strictValidation)
        assertFalse(config.outputEnabled)
        assertEquals("json", config.reportFormat)
        assertTrue(config.incremental)
    }

    @Test
    fun `ingestGovernance task wires governanceConfig from extension`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply(CodebasePlugin::class.java)

        val ext = project.extensions.getByType(CodebaseGovernanceExtension::class.java)
        ext.strictValidation.set(true)

        val task = project.tasks.getByName("ingestGovernance") as IngestGovernanceTask
        assertTrue(task.governanceConfig.get().strictValidation, "Task should inherit strictValidation from DSL")

        task.executeIngest()

        assertNotNull(task.lastIngestionReport)
    }

    @Test
    fun `CLI property overrides extension default`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "gradle.properties").writeText("codebase.governance.strictValidation=true")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.plugins.apply(CodebasePlugin::class.java)

        val task = project.tasks.getByName("ingestGovernance") as IngestGovernanceTask
        assertTrue(task.governanceConfig.get().strictValidation, "CLI property should enable strict validation")
    }
}
