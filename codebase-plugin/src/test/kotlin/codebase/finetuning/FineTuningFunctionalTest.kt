package codebase.finetuning

import codebase.koog.expert.ExpertRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FineTuningFunctionalTest {

    @Test
    fun `DSL block configures all 3 finetuning tasks end-to-end`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val ext = project.extensions.findByType(CodebaseFineTuningExtension::class.java)
        assertNotNull(ext)
        ext.baseModel.set("gpt-oss:120b-cloud")
        ext.dataset.set(listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"))
        ext.outputModelName.set("expert-cda")
        ext.corpusRatio.set(0.15)
        ext.maxIterations.set(5)
        ext.epochs.set(10)
        ext.learningRate.set(1e-4)
        ext.batchSize.set(8)
        ext.corpusGlobs.set(listOf("corpus/**/*.adoc"))
        ext.validationThreshold.set(0.85)

        val prepare = project.tasks.findByName("prepareFineTuningDataset") as PrepareFineTuningDatasetTask
        val fineTune = project.tasks.findByName("fineTuneExpert") as FineTuneExpertTask
        val publish = project.tasks.findByName("publishExpertToOllama") as PublishExpertToOllamaTask

        assertEquals("gpt-oss:120b-cloud", prepare.baseModel.get())
        assertEquals(listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"), prepare.dataset.get())
        assertEquals("expert-cda", prepare.outputModelName.get())
        assertEquals(0.15, prepare.corpusRatio.get())

        assertEquals("gpt-oss:120b-cloud", fineTune.baseModel.get())
        assertEquals("expert-cda", fineTune.outputModelName.get())
        assertEquals(0.15, fineTune.corpusRatio.get())

        assertEquals("expert-cda", publish.outputModelName.get())
    }

    @Test
    fun `prepareFineTuningDataset executes via DSL extension values`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val ext = project.extensions.findByType(CodebaseFineTuningExtension::class.java)!!
        ext.baseModel.set("gpt-oss:120b-cloud")
        ext.dataset.set(listOf("docs/afnor/**/*.adoc"))
        ext.outputModelName.set("expert-cda")
        ext.corpusRatio.set(0.20)

        val prepare = project.tasks.findByName("prepareFineTuningDataset") as PrepareFineTuningDatasetTask
        prepare.executePrepare()

        val outFile = prepare.outputFile.get().asFile
        assertTrue(outFile.exists())
        val content = outFile.readText()
        assertTrue(content.contains("FROM gpt-oss:120b-cloud"))
        assertTrue(content.contains("expert-cda"))
        assertTrue(content.contains("docs/afnor/**/*.adoc"))
        assertTrue(content.contains("corpus ratio: 0.2"))
    }

    @Test
    fun `publishExpertToOllama registers fine-tuned expert via DSL extension values`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val ext = project.extensions.findByType(CodebaseFineTuningExtension::class.java)!!
        ext.outputModelName.set("expert-cda")
        ext.baseModel.set("gpt-oss:120b-cloud")

        val publish = project.tasks.findByName("publishExpertToOllama") as PublishExpertToOllamaTask
        publish.domainName.set("cda")
        publish.domainLabel.set("CDA expert")
        publish.anonymizeEndpoints.set(false)
        publish.manifestOutput.set(project.layout.buildDirectory.file("experts/manifest.json"))

        val registry = ExpertRegistry()
        publish.expertRegistry = registry
        publish.executePublish()

        assertEquals(1, registry.size())
        val resolved = registry.resolveByName("cda")
        assertNotNull(resolved)
        assertEquals("expert-cda", resolved?.modelName)
        assertTrue(publish.manifestOutput.get().asFile.exists())
    }
}