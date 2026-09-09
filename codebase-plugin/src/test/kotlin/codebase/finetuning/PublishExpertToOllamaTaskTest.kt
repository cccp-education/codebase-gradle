package codebase.finetuning

import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertExposureTask
import codebase.koog.expert.ExpertRegistry
import codebase.koog.expert.ExpertRegistration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PublishExpertToOllamaTaskTest {

    @Test
    fun `task is abstract DefaultTask`() {
        val clazz = PublishExpertToOllamaTask::class.java
        assertEquals("PublishExpertToOllamaTask", clazz.simpleName)
        assertTrue(org.gradle.api.DefaultTask::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `task is annotated DisableCachingByDefault`() {
        val annotation = PublishExpertToOllamaTask::class.java
            .getAnnotation(org.gradle.work.DisableCachingByDefault::class.java)
        assertNotNull(annotation, "Should have @DisableCachingByDefault")
    }

    @Test
    fun `task exposes outputModelName domainLabel baseUrl manifestOutput inputs`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.domainName.set("coding")
        task.domainLabel.set("Coding expert — Referentiel")
        task.baseUrl.set("http://localhost:11437")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))

        assertEquals("expert-coding", task.outputModelName.get())
        assertEquals("coding", task.domainName.get())
        assertEquals("Coding expert — Referentiel", task.domainLabel.get())
        assertEquals("http://localhost:11437", task.baseUrl.get())
    }

    @Test
    fun `execute registers expert in registry and delegates manifest to ExpertExposureTask`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.domainName.set("coding")
        task.domainLabel.set("Coding expert — Referentiel")
        task.baseUrl.set("http://localhost:11440")
        task.anonymizeEndpoints.set(false)
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))

        val registry = ExpertRegistry()
        task.expertRegistry = registry

        task.executePublish()

        val resolved = registry.resolve(ExpertDomain("coding", "Coding expert — Referentiel"))
        assertNotNull(resolved, "Expert should be registered in the registry")
        assertEquals("expert-coding", resolved?.modelName)
        assertEquals("http://localhost:11440", resolved?.baseUrl)

        val manifestFile = task.manifestOutput.get().asFile
        assertTrue(manifestFile.exists(), "Manifest file should exist")
        val content = manifestFile.readText()
        assertTrue(content.contains("expert-coding"), "Manifest should reference the model name")
        assertTrue(content.contains("coding"), "Manifest should reference the domain")
        assertTrue(content.contains("http://localhost:11440"), "Manifest should reference the baseUrl (not anonymized)")
    }

    @Test
    fun `execute anonymizes endpoints when configured`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-content")
        task.domainName.set("content")
        task.domainLabel.set("Content expert")
        task.baseUrl.set("http://localhost:11442")
        task.anonymizeEndpoints.set(true)
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        task.expertRegistry = ExpertRegistry()
        task.executePublish()

        val content = task.manifestOutput.get().asFile.readText()
        assertTrue(content.contains("***anonymized***"), "Manifest should anonymize the endpoint")
        assertTrue(content.contains("expert-content"))
    }

    @Test
    fun `execute throws GradleException when outputModelName blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.domainName.set("coding")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        task.expertRegistry = ExpertRegistry()
        assertFailsWith<org.gradle.api.GradleException> { task.executePublish() }
    }

    @Test
    fun `execute throws GradleException when domainName blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        task.expertRegistry = ExpertRegistry()
        assertFailsWith<org.gradle.api.GradleException> { task.executePublish() }
    }

    @Test
    fun `execute creates parent directories of manifestOutput`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.domainName.set("coding")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/nested/deep/manifest.json"))

        task.expertRegistry = ExpertRegistry()
        task.executePublish()

        assertTrue(task.manifestOutput.get().asFile.exists())
    }

    @Test
    fun `execute default registry is created when not injected`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.domainName.set("coding")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        task.executePublish()

        val content = task.manifestOutput.get().asFile.readText()
        assertTrue(content.contains("expert-coding"), "Default registry should still produce manifest")
    }

    @Test
    fun `execute preserves existing registry entries when adding new expert`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        val registry = ExpertRegistry()
        registry.register(
            ExpertRegistration(
                domain = ExpertDomain("existing", "Existing expert"),
                modelName = "existing-model",
                baseUrl = "http://localhost:11450"
            )
        )
        task.expertRegistry = registry

        task.outputModelName.set("expert-new")
        task.domainName.set("new")
        task.domainLabel.set("New expert")
        task.anonymizeEndpoints.set(false)
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        task.executePublish()

        assertEquals(2, registry.size(), "Registry should contain 2 experts after publish")
        val content = task.manifestOutput.get().asFile.readText()
        assertTrue(content.contains("existing-model"), "Manifest should preserve existing expert")
        assertTrue(content.contains("expert-new"), "Manifest should contain new expert")
    }

    @Test
    fun `execute uses default timeoutSeconds 120`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("publishExpertToOllama", PublishExpertToOllamaTask::class.java).get()

        task.outputModelName.set("expert-coding")
        task.domainName.set("coding")
        task.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        val registry = ExpertRegistry()
        task.expertRegistry = registry
        task.executePublish()

        val resolved = registry.resolve(ExpertDomain("coding", "coding"))
        assertNotNull(resolved)
        assertEquals(120L, resolved?.timeoutSeconds)
    }
}