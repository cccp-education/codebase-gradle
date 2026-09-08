package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC OCR-QUALITY US-4 — additive doubt DDL and insert templates.
 *
 * The doubt columns ride on `codex_chunks` via `ALTER TABLE ... ADD
 * COLUMN IF NOT EXISTS` — zéro re-vectorization, the existing embedding
 * column and rows stay untouched (Loi de l'Économie d'Encre).
 */
class StoreStatementsDoubtTest {

    @Test
    fun `doubtSchema is additive ALTER TABLE IF NOT EXISTS statements`() {
        val stmts = StoreStatements.doubtSchema()
        assertEquals(2, stmts.size)
        assertTrue(stmts[0].contains("ALTER TABLE codex_documents ADD COLUMN IF NOT EXISTS avg_confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0"))
        assertTrue(stmts[1].contains("ALTER TABLE codex_chunks ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0"))
        assertTrue(stmts[1].contains("ADD COLUMN IF NOT EXISTS doubtful BOOLEAN NOT NULL DEFAULT FALSE"))
    }

    @Test
    fun `doubtSchema statements do not interpolate variables`() {
        StoreStatements.doubtSchema().forEach { s ->
            assertFalse(containsInterpolation(s), "DDL must not interpolate: $s")
        }
    }

    @Test
    fun `insertChunkWithDoubt binds confidence and doubtful at positions 6 and 7`() {
        val sql = StoreStatements.insertChunkWithDoubt()
        assertTrue(sql.contains("INSERT INTO codex_chunks"))
        assertTrue(sql.contains("document_id, chunk_index, chunk_text, section_path, heading_level, confidence, doubtful"))
        assertTrue(sql.contains("\$6, \$7"))
        assertTrue(sql.contains("RETURNING id"))
        assertFalse(containsInterpolation(sql), "insertChunkWithDoubt must not interpolate variables: $sql")
    }

    @Test
    fun `insertChunkWithDoubt bind count is 7`() {
        assertEquals(7, StoreStatements.insertChunkWithDoubtBindCount())
    }

    private fun containsInterpolation(sql: String): Boolean {
        val pattern = Regex("""\$\p{Alpha}""")
        return pattern.containsMatchIn(sql)
    }
}