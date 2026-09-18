package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC CB-PAGE-PROVENANCE US-1 — additive page-provenance DDL (D2).
 *
 * The `pages TEXT` column is added on `codex_chunks` via
 * `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — idempotent, zéro
 * re-vectorization, and present in the `CREATE TABLE` for fresh bases
 * (Loi de l'Économie d'Encre).
 */
class StoreStatementsProvenanceTest {

    @Test
    fun `provenanceSchema is additive ALTER TABLE with pages TEXT`() {
        val stmts = StoreStatements.provenanceSchema()
        assertEquals(1, stmts.size)
        assertTrue(stmts[0].contains("ALTER TABLE codex_chunks ADD COLUMN IF NOT EXISTS pages TEXT"))
    }

    @Test
    fun `provenanceSchema statements do not interpolate variables`() {
        StoreStatements.provenanceSchema().forEach { s ->
            assertFalse(containsInterpolation(s), "DDL must not interpolate: $s")
        }
    }

    @Test
    fun `initSchema declares pages TEXT for fresh bases`() {
        val stmts = StoreStatements.initSchema()
        assertTrue(stmts[2].contains("CREATE TABLE IF NOT EXISTS codex_chunks"))
        assertTrue(stmts[2].contains("pages TEXT"))
    }

    @Test
    fun `initSchema remains 3 parameterless DDL statements`() {
        val stmts = StoreStatements.initSchema()
        assertEquals(3, stmts.size)
        stmts.forEach { s -> assertFalse(containsInterpolation(s), "DDL must not interpolate: $s") }
    }

    private fun containsInterpolation(sql: String): Boolean {
        val pattern = Regex("""\$\p{Alpha}""")
        return pattern.containsMatchIn(sql)
    }
}