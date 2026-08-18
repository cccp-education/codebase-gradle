package codebase.finetuning

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * EPIC FT-PIPELINE US-1 — merger 4 sources.
 *
 * Precedence order: ENV vars < gradle.properties < YAML file < CLI -P params.
 * Each higher-priority source overrides the same key from lower-priority sources.
 * Pattern `CapsuleConfigMerger` (capsule) / `ValidationConfig` merge.
 */
class FineTuningConfigMergerTest {

    @TempDir
    lateinit var tempDir: File

    // ─── loadFromGradleProperties ────────────────────────────────

    @Test
    fun `loadFromGradleProperties reads codebase finetuning dot-prefixed properties`() {
        val projectDir = File(tempDir, "project").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=5
            codebase.finetuning.learningRate=1e-4
            codebase.finetuning.batchSize=8
            codebase.finetuning.corpusGlobs=docs/afnor/**/*.adoc,docs/reac/**/*.adoc
            codebase.finetuning.continualPreTrainingRatio=0.20
        """.trimIndent())

        val config = FineTuningConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(5, config.epochs)
        assertEquals(1e-4, config.learningRate)
        assertEquals(8, config.batchSize)
        assertEquals(listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"), config.corpusGlobs)
        assertEquals(0.20, config.continualPreTrainingRatio)
    }

    @Test
    fun `loadFromGradleProperties returns defaults when no gradle properties file`() {
        val projectDir = File(tempDir, "empty-project").also { it.mkdirs() }
        val config = FineTuningConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(3, config.epochs)
        assertEquals(2e-4, config.learningRate)
        assertEquals(4, config.batchSize)
        assertEquals(emptyList(), config.corpusGlobs)
        assertEquals(0.10, config.continualPreTrainingRatio)
    }

    @Test
    fun `loadFromGradleProperties ignores non-finetuning properties`() {
        val projectDir = File(tempDir, "mixed").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            org.gradle.jvmargs=-Xmx2g
            codebase.finetuning.epochs=7
            someOtherProperty=value
        """.trimIndent())

        val config = FineTuningConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(7, config.epochs, "codebase.finetuning.epochs should be read")
        assertEquals(4, config.batchSize, "batchSize should be default when not in properties")
    }

    @Test
    fun `loadFromGradleProperties ignores commented lines`() {
        val projectDir = File(tempDir, "comments").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            # codebase.finetuning.epochs=9
            codebase.finetuning.epochs=6
        """.trimIndent())

        val config = FineTuningConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(6, config.epochs)
    }

    @Test
    fun `loadFromGradleProperties skips malformed lines`() {
        val projectDir = File(tempDir, "malformed").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=6
            codebase.finetuning.batchSize
            codebase.finetuning.learningRate=3e-4
        """.trimIndent())

        val config = FineTuningConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(6, config.epochs)
        assertEquals(3e-4, config.learningRate)
        assertEquals(4, config.batchSize, "default (malformed line skipped)")
    }

    // ─── loadFromEnvironment ─────────────────────────────────────

    @Test
    fun `loadFromEnvironment resolves defaults when no env vars set`() {
        val config = FineTuningConfigMerger.loadFromEnvironment()

        assertEquals(3, config.epochs)
        assertEquals(2e-4, config.learningRate)
        assertEquals(4, config.batchSize)
        assertEquals(emptyList(), config.corpusGlobs)
        assertEquals(0.10, config.continualPreTrainingRatio)
    }

    // ─── merge (4 sources) ───────────────────────────────────────

    @Test
    fun `merge returns yaml config when no props or cli`() {
        val projectDir = File(tempDir, "merge1").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig(epochs = 5, learningRate = 1e-4)

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals(5, merged.epochs)
        assertEquals(1e-4, merged.learningRate)
    }

    @Test
    fun `merge CLI params override YAML config`() {
        val projectDir = File(tempDir, "merge2").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig(epochs = 5)
        val cliParams = mapOf("epochs" to 9)

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(9, merged.epochs, "CLI should override YAML")
    }

    @Test
    fun `merge YAML overrides gradle properties`() {
        val projectDir = File(tempDir, "merge3").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=5
            codebase.finetuning.batchSize=8
        """.trimIndent())

        val yamlConfig = FineTuningConfig(epochs = 7, batchSize = 16)

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals(7, merged.epochs, "YAML should override gradle.properties")
        assertEquals(16, merged.batchSize, "YAML should override gradle.properties")
    }

    @Test
    fun `merge CLI overrides both YAML and gradle properties`() {
        val projectDir = File(tempDir, "merge4").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=5
        """.trimIndent())

        val yamlConfig = FineTuningConfig(epochs = 7)
        val cliParams = mapOf("epochs" to 11)

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(11, merged.epochs, "CLI should override both")
    }

    @Test
    fun `merge uses defaults when all sources are empty`() {
        val projectDir = File(tempDir, "merge5").also { it.mkdirs() }

        val merged = FineTuningConfigMerger.merge(projectDir, FineTuningConfig(), emptyMap())

        assertEquals(3, merged.epochs)
        assertEquals(2e-4, merged.learningRate)
        assertEquals(4, merged.batchSize)
        assertEquals(0.10, merged.continualPreTrainingRatio)
    }

    @Test
    fun `merge handles partial CLI override`() {
        val projectDir = File(tempDir, "merge6").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig(epochs = 5, learningRate = 1e-4, batchSize = 8)
        val cliParams = mapOf("epochs" to 9)

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(9, merged.epochs, "CLI overrides epochs only")
        assertEquals(1e-4, merged.learningRate, "YAML should pass through")
        assertEquals(8, merged.batchSize, "YAML should pass through")
    }

    @Test
    fun `merge CLI string integer param is parsed as Int`() {
        val projectDir = File(tempDir, "merge7").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig()
        val cliParams = mapOf("epochs" to "12")

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(12, merged.epochs, "CLI String \"12\" must be parsed as Int 12")
    }

    @Test
    fun `merge CLI string double param is parsed as Double`() {
        val projectDir = File(tempDir, "merge8").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig()
        val cliParams = mapOf("learningRate" to "5e-4")

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(5e-4, merged.learningRate, "CLI String \"5e-4\" must be parsed as Double")
    }

    @Test
    fun `merge gradle properties override ENV defaults when no YAML loaded`() {
        val projectDir = File(tempDir, "merge9").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=6
            codebase.finetuning.batchSize=12
        """.trimIndent())

        val merged = FineTuningConfigMerger.merge(projectDir, FineTuningConfig(), emptyMap(), yamlLoaded = false)

        assertEquals(6, merged.epochs, "gradle.properties should provide value when no YAML")
        assertEquals(12, merged.batchSize, "gradle.properties should provide value when no YAML")
    }

    @Test
    fun `merge handles all 4 precedence levels for same key`() {
        val projectDir = File(tempDir, "merge10").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            codebase.finetuning.epochs=5
        """.trimIndent())

        val yamlConfig = FineTuningConfig(epochs = 7)

        val merged1 = FineTuningConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(7, merged1.epochs, "YAML overrides props")

        val merged2 = FineTuningConfigMerger.merge(projectDir, yamlConfig, mapOf("epochs" to 13))
        assertEquals(13, merged2.epochs, "CLI overrides both")
    }

    @Test
    fun `merge corpusGlobs CLI comma-split overrides YAML`() {
        val projectDir = File(tempDir, "merge11").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig(corpusGlobs = listOf("docs/old/**/*.adoc"))
        val cliParams = mapOf("corpusGlobs" to "docs/afnor/**/*.adoc,docs/reac/**/*.adoc")

        val merged = FineTuningConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(
            listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"),
            merged.corpusGlobs,
            "CLI comma-split should override YAML"
        )
    }

    @Test
    fun `merge corpusGlobs empty CLI string falls back to YAML`() {
        val projectDir = File(tempDir, "merge12").also { it.mkdirs() }
        val yamlConfig = FineTuningConfig(corpusGlobs = listOf("docs/afnor/**/*.adoc"))

        val merged = FineTuningConfigMerger.merge(
            projectDir, yamlConfig, mapOf("corpusGlobs" to "")
        )

        assertEquals(listOf("docs/afnor/**/*.adoc"), merged.corpusGlobs, "Empty CLI should fall back to YAML")
    }

    @Test
    fun `merge NoYaml with no props and no CLI returns defaults`() {
        val projectDir = File(tempDir, "merge13").also { it.mkdirs() }

        val merged = FineTuningConfigMerger.merge(projectDir, FineTuningConfig(), emptyMap(), yamlLoaded = false)

        assertEquals(3, merged.epochs, "NoYaml: default epochs should be 3")
        assertEquals(2e-4, merged.learningRate, "NoYaml: default learningRate should be 2e-4")
        assertEquals(4, merged.batchSize, "NoYaml: default batchSize should be 4")
        assertEquals(0.10, merged.continualPreTrainingRatio, "NoYaml: default ratio should be 0.10")
    }
}
