package codebase.finetuning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FineTuneExpertTaskTest {

    @Test
    fun `task is abstract DefaultTask`() {
        val clazz = FineTuneExpertTask::class.java
        assertEquals("FineTuneExpertTask", clazz.simpleName)
        assertTrue(org.gradle.api.DefaultTask::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `task is annotated DisableCachingByDefault`() {
        val annotation = FineTuneExpertTask::class.java
            .getAnnotation(org.gradle.work.DisableCachingByDefault::class.java)
        assertNotNull(annotation, "Should have @DisableCachingByDefault")
    }

    @Test
    fun `task exposes baseModel dataset outputModelName corpusRatio inputs`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.corpusRatio.set(0.12)
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))

        assertEquals("gpt-oss:120b-cloud", task.baseModel.get())
        assertEquals(listOf("docs/**/*.adoc"), task.dataset.get())
        assertEquals("expert-cda", task.outputModelName.get())
        assertEquals(0.12, task.corpusRatio.get())
    }

    @Test
    fun `execute writes success report when pipeline returns Success`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/afnor/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.corpusRatio.set(0.10)
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))

        val fake = FakeFineTuner()
        fake.enqueueResult(
            FineTuningResult.success(
                outputModelName = "expert-cda",
                ggufPath = project.layout.buildDirectory.get().asFile.resolve("finetuning/gguf/expert-cda.gguf").absolutePath,
                iterations = 3,
                validationScore = 0.92
            )
        )
        task.pipeline = fake

        task.executeFineTune()

        val report = task.outputReport.get().asFile
        assertTrue(report.exists(), "Report file should exist")
        val content = report.readText()
        assertTrue(content.contains("expert-cda"), "Report should reference output model name")
        assertTrue(content.contains("SUCCESS"), "Report should contain SUCCESS status")
        assertTrue(content.contains("0.92"), "Report should contain validation score")
        assertEquals(1, fake.callCount)
    }

    @Test
    fun `execute writes failure report when pipeline returns Failure`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-x")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))

        val fake = FakeFineTuner()
        fake.enqueueResult(FineTuningResult.failure("Ollama unavailable", listOf("docs/**/*.adoc")))
        task.pipeline = fake

        task.executeFineTune()

        val content = task.outputReport.get().asFile.readText()
        assertTrue(content.contains("FAILURE"), "Report should contain FAILURE status")
        assertTrue(content.contains("Ollama unavailable"), "Report should contain failure reason")
    }

    @Test
    fun `execute throws GradleException when baseModel blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-x")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))

        assertFailsWith<org.gradle.api.GradleException> { task.executeFineTune() }
    }

    @Test
    fun `execute throws GradleException when outputModelName blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))

        assertFailsWith<org.gradle.api.GradleException> { task.executeFineTune() }
    }

    @Test
    fun `execute throws GradleException when dataset empty`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.outputModelName.set("expert-x")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))

        assertFailsWith<org.gradle.api.GradleException> { task.executeFineTune() }
    }

    @Test
    fun `execute uses corpusGlobs fallback when dataset empty`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.corpusGlobs.set(listOf("corpus/**/*.adoc"))
        task.outputModelName.set("expert-fpa")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))

        val fake = FakeFineTuner()
        fake.enqueueResult(FineTuningResult.success("expert-fpa", "/tmp/x.gguf", 1, 1.0))
        task.pipeline = fake

        task.executeFineTune()

        assertEquals(listOf("corpus/**/*.adoc"), fake.lastRequest?.dataset)
    }

    @Test
    fun `execute creates parent directories of outputReport`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/nested/deep/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))

        val fake = FakeFineTuner()
        fake.enqueueResult(FineTuningResult.success("expert-cda", "/tmp/x.gguf", 1, 1.0))
        task.pipeline = fake

        task.executeFineTune()

        assertTrue(task.outputReport.get().asFile.exists())
    }

    @Test
    fun `execute report contains baseModel and dataset fields`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gemma4:31b-cloud")
        task.dataset.set(listOf("docs/cda/**/*.adoc", "docs/reac/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))

        val fake = FakeFineTuner()
        fake.enqueueResult(FineTuningResult.success("expert-cda", "/tmp/x.gguf", 2, 0.88))
        task.pipeline = fake

        task.executeFineTune()

        val content = task.outputReport.get().asFile.readText()
        assertTrue(content.contains("gemma4:31b-cloud"), "Report should reference base model")
        assertTrue(content.contains("docs/cda/**/*.adoc"), "Report should reference dataset glob 1")
        assertTrue(content.contains("docs/reac/**/*.adoc"), "Report should reference dataset glob 2")
    }

    @Test
    fun `execute default pipeline is OllamaFineTunerAdapter when not overridden`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("fineTuneExpert", FineTuneExpertTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.json"))
        task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))
        task.ollamaBaseUrl.set("http://localhost:11437")

        task.executeFineTune()

        val content = task.outputReport.get().asFile.readText()
        assertTrue(content.contains("FAILURE"), "Default Ollama adapter with no server should produce FAILURE")
    }
}