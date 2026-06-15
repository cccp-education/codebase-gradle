package codebase.koog.autofocus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutofocusLevelTest {

    @Test
    fun `BIG_PICTURE has highest token budget`() {
        val budgets = AutofocusLevel.entries.map { it.tokenBudget }
        assertEquals(8000, AutofocusLevel.BIG_PICTURE.tokenBudget)
        assertTrue(budgets.max() == AutofocusLevel.BIG_PICTURE.tokenBudget)
    }

    @Test
    fun `IMPLEMENTATION has lowest token budget`() {
        val budgets = AutofocusLevel.entries.map { it.tokenBudget }
        assertEquals(500, AutofocusLevel.IMPLEMENTATION.tokenBudget)
        assertTrue(budgets.min() == AutofocusLevel.IMPLEMENTATION.tokenBudget)
    }

    @Test
    fun `token budgets are strictly decreasing by level`() {
        val levels = AutofocusLevel.entries
        for (i in 0 until levels.size - 1) {
            assertTrue(
                levels[i].tokenBudget > levels[i + 1].tokenBudget,
                "${levels[i].name} (${levels[i].tokenBudget}) should be > ${levels[i + 1].name} (${levels[i + 1].tokenBudget})"
            )
        }
    }

    @Test
    fun `fromName returns correct level for exact match`() {
        assertEquals(AutofocusLevel.BIG_PICTURE, AutofocusLevel.fromName("BIG_PICTURE"))
        assertEquals(AutofocusLevel.ARCHITECTURE, AutofocusLevel.fromName("ARCHITECTURE"))
        assertEquals(AutofocusLevel.MODULE, AutofocusLevel.fromName("MODULE"))
        assertEquals(AutofocusLevel.IMPLEMENTATION, AutofocusLevel.fromName("IMPLEMENTATION"))
    }

    @Test
    fun `fromName is case insensitive`() {
        assertEquals(AutofocusLevel.BIG_PICTURE, AutofocusLevel.fromName("big_picture"))
        assertEquals(AutofocusLevel.ARCHITECTURE, AutofocusLevel.fromName("architecture"))
        assertEquals(AutofocusLevel.MODULE, AutofocusLevel.fromName("module"))
        assertEquals(AutofocusLevel.IMPLEMENTATION, AutofocusLevel.fromName("implementation"))
    }

    @Test
    fun `fromName returns null for unknown level`() {
        assertNull(AutofocusLevel.fromName("NONEXISTENT"))
        assertNull(AutofocusLevel.fromName(""))
        assertNull(AutofocusLevel.fromName("x1"))
    }

    @Test
    fun `metadataByLevel contains all four levels`() {
        val metadata = AutofocusLevel.metadataByLevel()
        assertEquals(4, metadata.size)
        AutofocusLevel.entries.forEach { level ->
            assertNotNull(metadata[level], "Missing metadata for ${level.name}")
        }
    }

    @Test
    fun `BIG_PICTURE metadata has governance sources`() {
        val metadata = AutofocusLevel.metadataByLevel()[AutofocusLevel.BIG_PICTURE]!!
        assertTrue(metadata.contextSources.any { it.contains("AGENTS") })
        assertTrue(metadata.contextSources.any { it.contains("INDEX") })
        assertTrue(metadata.contextSources.any { it.contains("PLAN_GLOBAL") })
        assertTrue(metadata.contextSources.any { it.contains("PROMPT_REPRISE") })
    }

    @Test
    fun `IMPLEMENTATION metadata targets single file`() {
        val metadata = AutofocusLevel.metadataByLevel()[AutofocusLevel.IMPLEMENTATION]!!
        assertTrue(metadata.summarization.contains("Code brut"))
        assertTrue(metadata.summarization.contains("pas de résumé"))
    }

    @Test
    fun `ARCHITECTURE filePattern targets Gradle files`() {
        val pattern = AutofocusLevel.ARCHITECTURE.filePattern
        assertTrue(pattern.contains("build.gradle.kts"))
        assertTrue(pattern.contains("settings.gradle.kts"))
        assertTrue(pattern.contains("libs.versions.toml"))
    }

    @Test
    fun `MODULE filePattern targets Kotlin files`() {
        assertTrue(AutofocusLevel.MODULE.filePattern.contains("*.kt"))
    }

    @Test
    fun `IMPLEMENTATION filePattern targets single Kotlin file`() {
        assertEquals("*.kt", AutofocusLevel.IMPLEMENTATION.filePattern)
    }

    @Test
    fun `BIG_PICTURE filePattern targets AsciiDoc files`() {
        assertTrue(AutofocusLevel.BIG_PICTURE.filePattern.contains("*.adoc"))
    }

    @Test
    fun `all levels have non-empty description`() {
        AutofocusLevel.entries.forEach { level ->
            assertTrue(level.description.isNotBlank(), "${level.name} description is blank")
        }
    }

    @Test
    fun `LevelMetadata data class equality works`() {
        val a = LevelMetadata(listOf("a", "b"), "summary")
        val b = LevelMetadata(listOf("a", "b"), "summary")
        assertEquals(a, b)
    }

    @Test
    fun `LevelMetadata data class copy works`() {
        val original = LevelMetadata(listOf("a"), "summary")
        val copied = original.copy(contextSources = listOf("a", "b"))
        assertEquals(listOf("a", "b"), copied.contextSources)
        assertEquals("summary", copied.summarization)
    }
}
