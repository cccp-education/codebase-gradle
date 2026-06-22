package codebase.koog.expert

import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExpertExposureRoundTripTest {

    @Test
    fun `task generates manifest and reader parses it back`() {
        val projectDir = Files.createTempDirectory("epic8-roundtrip").toFile()
        try {
            val project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .withName("epic8-roundtrip")
                .build()
            project.pluginManager.apply("java-base")

            val registry = ExpertRegistry().apply {
                registerAll(listOf(
                    ExpertRegistration(
                        ExpertDomain("kotlin", "kotlin domain"),
                        "gpt-oss:120b-cloud",
                        "http://localhost:11437",
                        120
                    ),
                    ExpertRegistration(
                        ExpertDomain("docs", "docs domain"),
                        "qwen3-vl:235b-cloud",
                        "http://localhost:11438",
                        90
                    )
                ))
            }

            val task = project.tasks
                .register("exposeExperts", ExpertExposureTask::class.java).get()
            task.expertRegistry = registry
            val manifestFile = project.layout.buildDirectory
                .file("experts/exposure-manifest.json").get().asFile
            task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
            task.anonymizeEndpoints.set(false)

            task.executeExposure()

            assertTrue(manifestFile.exists(), "Manifest file should exist after task execution")

            val parsed = ExpertManifestReader.read(manifestFile)

            assertEquals("1.0", parsed.version)
            assertEquals(2, parsed.experts.size)
            val kotlinEntry = parsed.findByDomain("kotlin")
            assertNotNull(kotlinEntry)
            assertEquals("gpt-oss:120b-cloud", kotlinEntry.modelName)
            assertEquals("http://localhost:11437", kotlinEntry.baseUrl)
            assertEquals(120L, kotlinEntry.timeoutSeconds)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `task anonymized manifest is readable and hides baseUrl`() {
        val projectDir = Files.createTempDirectory("epic8-anon-roundtrip").toFile()
        try {
            val project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .withName("epic8-anon-roundtrip")
                .build()
            project.pluginManager.apply("java-base")

            val registry = ExpertRegistry().apply {
                registerAll(listOf(
                    ExpertRegistration(
                        ExpertDomain("docs", "docs domain"),
                        "qwen3-vl:235b-cloud",
                        "http://localhost:11438",
                        90
                    )
                ))
            }

            val task = project.tasks
                .register("exposeExperts", ExpertExposureTask::class.java).get()
            task.expertRegistry = registry
            task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
            task.anonymizeEndpoints.set(true)

            task.executeExposure()

            val manifestFile = project.layout.buildDirectory
                .file("experts/exposure-manifest.json").get().asFile
            val parsed = ExpertManifestReader.read(manifestFile)

            val docsEntry = parsed.findByDomain("docs")
            assertNotNull(docsEntry)
            assertEquals("***anonymized***", docsEntry.baseUrl)
        } finally {
            projectDir.deleteRecursively()
        }
    }
}