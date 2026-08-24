package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Characterization test — EPIC CDX-RAG-1 : mirror of codex `IngestStatementsTest`.
 *
 * Verbatim migration of `codex.store.IngestStatements` into `codebase.store`
 * as `StoreStatements` (Brooklyn → Queens, N2 → N1). The SQL templates and
 * bind-count contract are identical — only the package and class name change.
 */
class StoreStatementsTest {

    @Test
    fun `initSchema statements are parameterless DDL`() {
        val stmts = StoreStatements.initSchema()
        assertEquals(3, stmts.size)
        assertTrue(stmts[0].contains("CREATE EXTENSION IF NOT EXISTS vector"))
        assertTrue(stmts[1].contains("CREATE TABLE IF NOT EXISTS codex_documents"))
        assertTrue(stmts[2].contains("CREATE TABLE IF NOT EXISTS codex_chunks"))
        stmts.forEach { s -> assertFalse(containsInterpolation(s), "DDL must not interpolate: $s") }
    }

    @Test
    fun `insertDocument uses 3 positional parameters and no interpolation`() {
        val sql = StoreStatements.insertDocument()
        assertTrue(sql.contains("$1"), "insertDocument should bind source_document at $1")
        assertTrue(sql.contains("$2"), "insertDocument should bind chunk_count at $2")
        assertTrue(sql.contains("$3"), "insertDocument should bind license at $3")
        assertTrue(sql.contains("RETURNING id"))
        assertFalse(containsInterpolation(sql), "insertDocument must not interpolate variables: $sql")
    }

    @Test
    fun `insertChunk uses 5 positional parameters and no interpolation`() {
        val sql = StoreStatements.insertChunk()
        assertTrue(sql.contains("$1"), "insertChunk should bind document_id at $1")
        assertTrue(sql.contains("$2"), "insertChunk should bind chunk_index at $2")
        assertTrue(sql.contains("$3"), "insertChunk should bind chunk_text at $3")
        assertTrue(sql.contains("$4"), "insertChunk should bind section_path at $4")
        assertTrue(sql.contains("$5"), "insertChunk should bind heading_level at $5")
        assertTrue(sql.contains("RETURNING id"))
        assertFalse(containsInterpolation(sql), "insertChunk must not interpolate variables: $sql")
    }

    @Test
    fun `updateEmbedding inlines safe vector literal and chunkId`() {
        val sql = StoreStatements.updateEmbedding("0.1,0.2,0.3", 42L)
        assertTrue(
            sql.contains("'[0.1,0.2,0.3]'::vector"),
            "updateEmbedding must inline vector literal, was: $sql"
        )
        assertTrue(
            sql.contains("WHERE id = 42"),
            "updateEmbedding must inline chunkId (Long safe), was: $sql"
        )
        assertFalse(
            containsInterpolation(sql),
            "updateEmbedding must not interpolate Kotlin variables (CDX-CR3-1): $sql"
        )
    }

    @Test
    fun `updateEmbedding with chunkId documents the safe source contract`() {
        val sql = StoreStatements.updateEmbedding("1.0", 999L)
        assertTrue(sql.contains("WHERE id = 999"))
        assertFalse(containsInterpolation(sql))
    }

    @Test
    fun `insertDocument bind count is 3`() {
        assertEquals(3, StoreStatements.insertDocumentBindCount())
    }

    @Test
    fun `updateEmbedding bind count is 0`() {
        assertEquals(0, StoreStatements.updateEmbeddingBindCount())
    }

    @Test
    fun `insertChunk bind count is 5`() {
        assertEquals(5, StoreStatements.insertChunkBindCount())
    }

    /**
     * Détecte une interpolation Kotlin (`$identifier` hors de la portée d'un
     * paramètre positionnel `$N`). Un template SQL valide ne contient que
     * des `$1`, `$2`... ou aucune variable.
     */
    private fun containsInterpolation(sql: String): Boolean {
        val pattern = Regex("""\$\p{Alpha}""")
        return pattern.containsMatchIn(sql)
    }
}