package codebase.finetuning

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * EPIC FT-PIPELINE US-1 — domaine `codebase.finetuning` (7ème).
 *
 * `FineTuningConfig` data class immutable + invariants.
 * Pattern `ValidationConfig` (capsule) / `QualityGateConfig` (codebase).
 */
class FineTuningConfigTest {

    @Test
    fun `defaults are retro-compatible`() {
        val config = FineTuningConfig()

        assertEquals(3, config.epochs)
        assertEquals(2e-4, config.learningRate)
        assertEquals(4, config.batchSize)
        assertTrue(config.corpusGlobs.isEmpty())
        assertEquals(0.10, config.continualPreTrainingRatio)
    }

    @Test
    fun `copy preserves defaults`() {
        val base = FineTuningConfig()
        val tuned = base.copy(epochs = 5)

        assertEquals(5, tuned.epochs)
        assertEquals(2e-4, tuned.learningRate)
        assertEquals(4, tuned.batchSize)
        assertEquals(0.10, tuned.continualPreTrainingRatio)
    }

    @Test
    fun `epochs must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(epochs = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(epochs = -1)
        }
    }

    @Test
    fun `learningRate must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(learningRate = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(learningRate = -1e-4)
        }
    }

    @Test
    fun `batchSize must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(batchSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(batchSize = -8)
        }
    }

    @Test
    fun `continualPreTrainingRatio must be in 0 to 1`() {
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(continualPreTrainingRatio = -0.01)
        }
        assertFailsWith<IllegalArgumentException> {
            FineTuningConfig(continualPreTrainingRatio = 1.01)
        }
    }

    @Test
    fun `boundary values are accepted`() {
        val config = FineTuningConfig(
            epochs = 1,
            learningRate = 1e-6,
            batchSize = 1,
            continualPreTrainingRatio = 0.0
        )
        assertEquals(1, config.epochs)
        assertEquals(0.0, config.continualPreTrainingRatio)
    }

    @Test
    fun `corpusGlobs accepts a non-empty list`() {
        val config = FineTuningConfig(corpusGlobs = listOf("docs/afnor/**/*.adoc"))
        assertEquals(listOf("docs/afnor/**/*.adoc"), config.corpusGlobs)
    }
}
