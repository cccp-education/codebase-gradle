package codebase.finetuning

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodebaseFineTuningExtensionTest {

    @Test
    fun `extension is registered under name fineTuning`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("java-base")
        project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        val ext = project.extensions.findByName("fineTuning")
        assertNotNull(ext, "Extension 'fineTuning' should be registered")
        assertTrue(ext is CodebaseFineTuningExtension)
    }

    @Test
    fun `defaults are backward compat - baseModel empty`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        assertEquals("", ext.baseModel.get())
        assertTrue(ext.dataset.get().isEmpty())
        assertEquals("", ext.outputModelName.get())
        assertEquals(0.10, ext.corpusRatio.get())
        assertEquals(3, ext.maxIterations.get())
        assertEquals(3, ext.epochs.get())
        assertEquals(2e-4, ext.learningRate.get())
        assertEquals(4, ext.batchSize.get())
        assertTrue(ext.corpusGlobs.get().isEmpty())
        assertEquals(0.7, ext.validationThreshold.get())
    }

    @Test
    fun `DSL setters mutate properties`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

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

        assertEquals("gpt-oss:120b-cloud", ext.baseModel.get())
        assertEquals(listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"), ext.dataset.get())
        assertEquals("expert-cda", ext.outputModelName.get())
        assertEquals(0.15, ext.corpusRatio.get())
        assertEquals(5, ext.maxIterations.get())
        assertEquals(10, ext.epochs.get())
        assertEquals(1e-4, ext.learningRate.get())
        assertEquals(8, ext.batchSize.get())
        assertEquals(listOf("corpus/**/*.adoc"), ext.corpusGlobs.get())
        assertEquals(0.85, ext.validationThreshold.get())
    }

    @Test
    fun `toConfig maps extension to FineTuningConfig`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.epochs.set(7)
        ext.learningRate.set(3e-4)
        ext.batchSize.set(16)
        ext.corpusGlobs.set(listOf("corpus/**/*.adoc"))
        ext.corpusRatio.set(0.20)

        val config = ext.toConfig()
        assertEquals(7, config.epochs)
        assertEquals(3e-4, config.learningRate)
        assertEquals(16, config.batchSize)
        assertEquals(listOf("corpus/**/*.adoc"), config.corpusGlobs)
        assertEquals(0.20, config.continualPreTrainingRatio)
    }

    @Test
    fun `toConfig defaults match FineTuningConfig defaults`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        val config = ext.toConfig()
        assertEquals(FineTuningConfig(), config)
    }

    @Test
    fun `toRequest builds FineTuningRequest from extension fields`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.baseModel.set("gemma4:31b-cloud")
        ext.dataset.set(listOf("docs/cda/**/*.adoc"))
        ext.outputModelName.set("expert-cda")
        ext.corpusRatio.set(0.12)

        val request = ext.toRequest()
        assertEquals("gemma4:31b-cloud", request.baseModel)
        assertEquals(listOf("docs/cda/**/*.adoc"), request.dataset)
        assertEquals("expert-cda", request.outputModelName)
        assertEquals(0.12, request.corpusRatio)
    }

    @Test
    fun `toRequest uses corpusGlobs as dataset fallback when dataset empty`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.baseModel.set("gpt-oss:120b-cloud")
        ext.corpusGlobs.set(listOf("corpus/**/*.adoc", "data/**/*.adoc"))
        ext.outputModelName.set("expert-fpa")

        val request = ext.toRequest()
        assertEquals(listOf("corpus/**/*.adoc", "data/**/*.adoc"), request.dataset)
    }

    @Test
    fun `toRequest throws when baseModel blank`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.dataset.set(listOf("docs/**/*.adoc"))
        ext.outputModelName.set("expert-x")

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { ext.toRequest() }
    }

    @Test
    fun `toRequest throws when outputModelName blank`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.baseModel.set("gpt-oss:120b-cloud")
        ext.dataset.set(listOf("docs/**/*.adoc"))

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { ext.toRequest() }
    }

    @Test
    fun `toRequest throws when dataset and corpusGlobs both empty`() {
        val project = ProjectBuilder.builder().build()
        val ext = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        ext.baseModel.set("gpt-oss:120b-cloud")
        ext.outputModelName.set("expert-x")

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { ext.toRequest() }
    }
}