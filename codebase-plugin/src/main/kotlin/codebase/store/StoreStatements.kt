package codebase.store

/**
 * Centralisation des templates SQL du pipeline d'ingest du store vectoriel.
 *
 * Verbatim migration of `codex.store.IngestStatements` into `codebase.store`
 * as `StoreStatements` (EPIC CDX-RAG-1, Brooklyn → Queens, N2 → N1).
 *
 * Les templates utilisent exclusivement des paramètres positionnels
 * (`$1`, `$2`...) bindés par R2DBC. Aucune interpolation de variable Kotlin
 * n'est autorisée. L'objet est pur (sans état, sans effet de bord) et donc
 * unit-testable sans base de données.
 *
 * Les compteurs de binds (`*BindCount`) documentent le contrat de binding
 * R2DBC attendu par chaque template.
 */
object StoreStatements {

    /** DDL d'initialisation du schéma pgvector (extension + 2 tables). */
    fun initSchema(): List<String> = listOf(
        "CREATE EXTENSION IF NOT EXISTS vector",
        "CREATE TABLE IF NOT EXISTS codex_documents (id BIGSERIAL PRIMARY KEY, source_document TEXT NOT NULL, chunk_count INTEGER NOT NULL, license TEXT NOT NULL, created_at TIMESTAMPTZ DEFAULT NOW())",
        "CREATE TABLE IF NOT EXISTS codex_chunks (id BIGSERIAL PRIMARY KEY, document_id BIGINT REFERENCES codex_documents(id) ON DELETE CASCADE, chunk_index INTEGER NOT NULL, chunk_text TEXT NOT NULL, section_path TEXT NOT NULL, heading_level INTEGER DEFAULT 0, embedding vector(384), created_at TIMESTAMPTZ DEFAULT NOW())"
    )

    /**
     * DDL additif des métadonnées de doute (EPIC OCR-QUALITY US-4).
     *
     * `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — idempotent, ne touche
     * ni l'embedding ni les lignes existantes (zéro re-vectorisation).
     * Les defaults garantissent que les chunks déjà ingérés (sans doute
     * connu) restent lus comme "confiants" : avg_confidence=1.0,
     * confidence=1.0, doubtful=FALSE.
     */
    fun doubtSchema(): List<String> = listOf(
        "ALTER TABLE codex_documents ADD COLUMN IF NOT EXISTS avg_confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0",
        "ALTER TABLE codex_chunks ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0, ADD COLUMN IF NOT EXISTS doubtful BOOLEAN NOT NULL DEFAULT FALSE"
    )

    /**
     * INSERT du document source avec RETURNING id.
     * Binds : (1=source_document, 2=chunk_count, 3=license).
     */
    fun insertDocument(): String =
        "INSERT INTO codex_documents (source_document, chunk_count, license) VALUES ($1, $2, $3) RETURNING id"

    /**
     * INSERT d'un chunk avec RETURNING id.
     * Binds : (1=document_id, 2=chunk_index, 3=chunk_text, 4=section_path, 5=heading_level).
     */
    fun insertChunk(): String =
        "INSERT INTO codex_chunks (document_id, chunk_index, chunk_text, section_path, heading_level) VALUES ($1, $2, $3, $4, $5) RETURNING id"

    /**
     * INSERT d'un chunk avec métadonnées de doute (EPIC OCR-QUALITY US-4).
     * Binds : (1=document_id, 2=chunk_index, 3=chunk_text, 4=section_path,
     * 5=heading_level, 6=confidence, 7=doubtful).
     */
    fun insertChunkWithDoubt(): String =
        "INSERT INTO codex_chunks (document_id, chunk_index, chunk_text, section_path, heading_level, confidence, doubtful) VALUES (\$1, \$2, \$3, \$4, \$5, \$6, \$7) RETURNING id"

    /**
     * Construit le SQL d'UPDATE de l'embedding pour un chunk.
     *
     * R2DBC/pgvector ne supporte pas le binding paramétré du vecteur dans
     * un contexte `SET embedding = $1::vector` (pgvector n'expose pas de
     * cast implicite `text → vector` en assignation, et le binding du
     * chunkId via `$1` échoue quand un littéral vectoriel est présent dans
     * la même requête — limitation R2DBC PostgreSQL). Les deux valeurs
     * sont donc inlinées comme littéraux SQL safe : le vecteur est généré
     * par `AllMiniLmL6V2EmbeddingModel` (ONNX, nombres sûrs), le chunkId
     * est un Long généré par PostgreSQL (`RETURNING id`).
     *
     * @param vectorLiteral le littéral vectoriel safe — nombres joinus par
     *        `,` avec ou sans brackets `[...]` (les brackets sont retirés :
     *        `computeEmbedding` les inclut pour le binding `$1` du SELECT,
     *        le SQL inline ne doit en porter qu'une seule paire)
     * @param chunkId l'id du chunk (Long généré par PostgreSQL, safe)
     * @return le SQL d'UPDATE de l'embedding
     */
    fun updateEmbedding(vectorLiteral: String, chunkId: Long): String =
        "UPDATE codex_chunks SET embedding = '[${vectorLiteral.trim('[', ']')}]'::vector WHERE id = $chunkId"

    /** Nombre de binds attendus pour [insertDocument]. */
    fun insertDocumentBindCount(): Int = 3

    /** Nombre de binds attendus pour [insertChunk]. */
    fun insertChunkBindCount(): Int = 5

    /** Nombre de binds attendus pour [insertChunkWithDoubt]. */
    fun insertChunkWithDoubtBindCount(): Int = 7

    /** Nombre de binds attendus pour [updateEmbedding] (aucun — littéraux safe). */
    fun updateEmbeddingBindCount(): Int = 0
}