package codebase.koog.expert

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExpertExposureTaskTest {

    private val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    private val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")
    private val generalDomain = ExpertDomain("general", "Generalist fallback")

    @Test
    fun `task generates manifest from registry`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("exposure-test")
            .build()
        project.pluginManager.apply("java-base")

        val registry = ExpertRegistry()
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11440", 120),
            ExpertRegistration(docsDomain, "gpt-oss:120b-cloud", "http://localhost:11441", 90)
        ))

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        assertTrue(outputFile.exists(), "Output file should exist: ${outputFile.absolutePath}")
        val content = outputFile.readText()
        assertTrue(content.contains("gpt-oss:120b-cloud"))
        assertTrue(content.contains("http://localhost:11440"))
        assertTrue(content.contains("kotlin"))
        assertTrue(content.contains("docs"))
    }

    @Test
    fun `task anonymizes endpoints when configured`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("exposure-anon")
            .build()
        project.pluginManager.apply("java-base")

        val registry = ExpertRegistry()
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11443", 120))

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(true)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        val content = outputFile.readText()
        assertTrue(content.contains("***anonymized***"))
        assertTrue(content.contains("gpt-oss:120b-cloud"))
        assertTrue(content.contains("kotlin"))
    }

    @Test
    fun `task filters by domains`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("exposure-filter")
            .build()
        project.pluginManager.apply("java-base")

        val registry = ExpertRegistry()
        registry.registerAll(listOf(
            ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11444", 120),
            ExpertRegistration(docsDomain, "gpt-oss:120b-cloud", "http://localhost:11445", 90),
            ExpertRegistration(generalDomain, "gpt-oss:120b-cloud", "http://localhost:11446", 60)
        ))

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.domains.set(listOf("kotlin", "docs"))
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        val content = outputFile.readText()
        assertTrue(content.contains("kotlin"))
        assertTrue(content.contains("docs"))
        assertTrue(!content.contains("general"), "Filtered domain 'general' should not appear")
    }

    @Test
    fun `task empty registry produces empty manifest`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("exposure-empty")
            .build()
        project.pluginManager.apply("java-base")

        val registry = ExpertRegistry()

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        val content = outputFile.readText()
        assertTrue(content.contains("\"experts\""))
        assertTrue(content.contains("[]"))
    }

    @Test
    fun `task output is valid JSON`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("exposure-json")
            .build()
        project.pluginManager.apply("java-base")

        val registry = ExpertRegistry()
        registry.register(ExpertRegistration(kotlinDomain, "gpt-oss:120b-cloud", "http://localhost:11447", 120))

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        val content = outputFile.readText()
        assertTrue(content.startsWith("{"), "Should start with {")
        assertTrue(content.trimEnd().endsWith("}"), "Should end with }")
        assertTrue(content.contains("\"version\""))
        assertTrue(content.contains("\"generatedAt\""))
        assertTrue(content.contains("\"experts\""))
    }

    @Test
    fun `task class is abstract DefaultTask`() {
        val clazz = ExpertExposureTask::class.java
        assertEquals("ExpertExposureTask", clazz.simpleName)
        assertTrue(org.gradle.api.DefaultTask::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `task has DisableCachingByDefault annotation`() {
        val clazz = ExpertExposureTask::class.java
        val annotation = clazz.getAnnotation(org.gradle.work.DisableCachingByDefault::class.java)
        assertNotNull(annotation, "Should have @DisableCachingByDefault")
    }
}
