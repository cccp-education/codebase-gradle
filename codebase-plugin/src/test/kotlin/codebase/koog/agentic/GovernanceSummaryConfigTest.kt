package codebase.koog.agentic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GovernanceSummaryConfigTest {

    @Test
    fun `defaults are retro-compatible`() {
        val config = GovernanceSummaryConfig()

        assertFalse(config.strictValidation, "strictValidation should default to false")
        assertTrue(config.outputEnabled, "outputEnabled should default to true")
        assertEquals("json", config.reportFormat)
    }

    @Test
    fun `copy preserves defaults`() {
        val base = GovernanceSummaryConfig()
        val strict = base.copy(strictValidation = true)

        assertTrue(strict.strictValidation)
        assertTrue(strict.outputEnabled)
        assertEquals("json", strict.reportFormat)
    }

    @Test
    fun `can disable output`() {
        val config = GovernanceSummaryConfig(outputEnabled = false)

        assertFalse(config.outputEnabled)
    }
}
