package codebase.store

/**
 * Assignation des `chunk_index` locaux par document.
 *
 * Verbatim migration of `codex.store.IngestIndexing` into `codebase.store`
 * (EPIC CDX-RAG-1, Brooklyn → Queens, N2 → N1).
 *
 * Garantit que deux chunks identiques dans le même document reçoivent des
 * `chunk_index` distincts (0, 1, 2...) via un `withIndex()` par groupe de
 * document, indépendant de l'égalité structurelle des chunks. L'objet est
 * pur (sans état, sans effet de bord) et donc unit-testable sans base de
 * données.
 */
object IngestIndexing {

    /**
     * Associe chaque `sourceDocument` à la liste de ses `chunk_index`
     * locaux (0, 1, 2...), dans l'ordre d'apparition.
     *
     * @param chunks liste globale des chunks (potentiellement multi-documents)
     * @return map `sourceDocument → List<Int>` des index locaux séquentiels
     */
    fun assignLocalIndices(chunks: List<DocumentChunk>): Map<String, List<Int>> =
        chunks
            .groupBy { it.sourceDocument }
            .mapValues { (_, docChunks) -> docChunks.indices.toList() }
}