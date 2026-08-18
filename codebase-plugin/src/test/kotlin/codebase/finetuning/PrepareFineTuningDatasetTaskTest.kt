package codebase.finetuning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PrepareFineTuningDatasetTaskTest {

    @Test
    fun `task is abstract DefaultTask`() {
        val clazz = PrepareFineTuningDatasetTask::class.java
        assertEquals("PrepareFineTuningDatasetTask", clazz.simpleName)
        assertTrue(org.gradle.api.DefaultTask::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun `task is annotated DisableCachingByDefault`() {
        val annotation = PrepareFineTuningDatasetTask::class.java
            .getAnnotation(org.gradle.work.DisableCachingByDefault::class.java)
        assertNotNull(annotation, "Should have @DisableCachingByDefault")
    }

    @Test
    fun `task exposes baseModel dataset outputModelName corpusRatio inputs`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepareFineTuningDataset", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/afnor/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.corpusRatio.set(0.15)
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/dataset.txt"))

        assertEquals("gpt-oss:120b-cloud", task.baseModel.get())
        assertEquals(listOf("docs/afnor/**/*.adoc"), task.dataset.get())
        assertEquals("expert-cda", task.outputModelName.get())
        assertEquals(0.15, task.corpusRatio.get())
    }

    @Test
    fun `execute writes dataset listing and Modelfile section to outputFile`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.corpusRatio.set(0.10)
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/dataset-prep.txt"))

        task.executePrepare()

        val out = task.outputFile.get().asFile
        assertTrue(out.exists(), "Output file should exist")
        val content = out.readText()
        assertTrue(content.contains("FROM gpt-oss:120b-cloud"), "Should embed base model in Modelfile section")
        assertTrue(content.contains("expert-cda"), "Should reference output model name")
        assertTrue(content.contains("docs/afnor/**/*.adoc"), "Should list dataset glob 1")
        assertTrue(content.contains("docs/reac/**/*.adoc"), "Should list dataset glob 2")
        assertTrue(content.contains("corpus ratio: 0.1"), "Should include corpus ratio")
    }

    @Test
    fun `execute uses corpusGlobs fallback when dataset empty`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gemma4:31b-cloud")
        task.corpusGlobs.set(listOf("corpus/**/*.adoc"))
        task.outputModelName.set("expert-fpa")
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/dataset-prep.txt"))

        task.executePrepare()

        val content = task.outputFile.get().asFile.readText()
        assertTrue(content.contains("corpus/**/*.adoc"))
        assertTrue(content.contains("FROM gemma4:31b-cloud"))
    }

    @Test
    fun `execute throws GradleException when baseModel blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-x")
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/out.txt"))

        assertFailsWith<org.gradle.api.GradleException> { task.executePrepare() }
    }

    @Test
    fun `execute throws GradleException when outputModelName blank`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/out.txt"))

        assertFailsWith<org.gradle.api.GradleException> { task.executePrepare() }
    }

    @Test
    fun `execute throws GradleException when dataset and corpusGlobs both empty`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.outputModelName.set("expert-x")
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/out.txt"))

        assertFailsWith<org.gradle.api.GradleException> { task.executePrepare() }
    }

    @Test
    fun `execute creates parent directories of outputFile`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/nested/deep/dataset-prep.txt"))

        task.executePrepare()

        assertTrue(task.outputFile.get().asFile.exists())
    }

    @Test
    fun `execute Modelfile header uses corpusRatio value`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.corpusRatio.set(0.25)
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/out.txt"))

        task.executePrepare()

        val content = task.outputFile.get().asFile.readText()
        assertTrue(content.contains("0.25"), "Modelfile should embed corpusRatio=0.25")
    }

    @Test
    fun `execute writes dataset section header and Modelfile section header`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply("java-base")
        val task = project.tasks.register("prepare", PrepareFineTuningDatasetTask::class.java).get()

        task.baseModel.set("gpt-oss:120b-cloud")
        task.dataset.set(listOf("docs/**/*.adoc"))
        task.outputModelName.set("expert-cda")
        task.outputFile.set(project.layout.buildDirectory.file("finetuning/out.txt"))

        task.executePrepare()

        val content = task.outputFile.get().asFile.readText()
        assertTrue(content.contains("Modelfile"), "Should contain Modelfile section")
        assertTrue(content.contains("Dataset"), "Should contain Dataset section")
    }
}