package codebase.koog.agentic

class FakeAgenticChunkRepository : AgenticChunkRepository {

    private val chunks = mutableMapOf<String, OntologizedChunk>()
    private val relations = mutableListOf<ChunkRelation>()
    private var nextRelationId = 1L

    override suspend fun initSchema() {}

    override suspend fun insertChunk(chunk: OntologizedChunk): Boolean {
        if (chunks.containsKey(chunk.chunk.id)) return false
        chunks[chunk.chunk.id] = chunk
        return true
    }

    override suspend fun insertChunks(chunks: List<OntologizedChunk>): Int {
        var inserted = 0
        for (chunk in chunks) {
            if (insertChunk(chunk)) inserted++
        }
        return inserted
    }

    override suspend fun getChunk(id: String): OntologizedChunk? = chunks[id]

    override suspend fun listChunks(limit: Int): List<OntologizedChunk> =
        chunks.values.take(limit)

    override suspend fun listChunksByDomain(domain: String, limit: Int): List<OntologizedChunk> =
        chunks.values.filter { it.chunk.domain == domain }.take(limit)

    override suspend fun listChunksByVerb(verb: TaxonomyVerb, limit: Int): List<OntologizedChunk> =
        chunks.values.filter { it.chunk.verb == verb }.take(limit)

    override suspend fun listChunksByDagLevel(level: DagLevel, limit: Int): List<OntologizedChunk> =
        chunks.values.filter { it.chunk.dagLevel == level }.take(limit)

    override suspend fun listChunksByTaxonomySection(section: TaxonomySection, limit: Int): List<OntologizedChunk> =
        chunks.values.filter { it.taxonomySection == section }.take(limit)

    override suspend fun insertRelation(
        sourceChunkId: String,
        targetChunkId: String,
        relationType: ChunkRelationType,
        confidence: Double
    ): Long {
        val id = nextRelationId++
        relations.add(ChunkRelation(id, sourceChunkId, targetChunkId, relationType, confidence, java.time.Instant.now()))
        return id
    }

    override suspend fun insertRelations(relations: List<ChunkRelation>): Int {
        var inserted = 0
        for (rel in relations) {
            insertRelation(rel.sourceChunkId, rel.targetChunkId, rel.relationType, rel.confidence)
            inserted++
        }
        return inserted
    }

    override suspend fun getRelations(sourceChunkId: String): List<ChunkRelation> =
        relations.filter { it.sourceChunkId == sourceChunkId }

    override suspend fun updateEmbedding(id: String, vectorStr: String): Boolean {
        val chunk = chunks[id] ?: return false
        return true
    }

    override suspend fun countChunks(): Int = chunks.size

    override suspend fun countRelations(): Int = relations.size
}
