package codebase.koog.agentic

data class IngestionReport(
    val filesScanned: Int,
    val chunksAdded: Int,
    val chunksSkipped: Int,
    val chunksModified: Int,
    val artifactsCompiled: Int,
    val sectionsAdded: Map<GovernanceSection, Int> = emptyMap(),
    val sectionsTotal: Map<GovernanceSection, Int> = emptyMap()
)

class AgenticIngestor(
    private val chunker: AgenticChunker = AgenticChunker(),
    private val ontologizer: AgenticOntologizer = AgenticOntologizer(),
    private val repository: AgenticChunkRepository,
    private val compiler: AgenticCompiler = AgenticCompiler(),
    private val governanceOntologizer: GovernanceOntologizer? = null
) {

    suspend fun ingest(files: List<Pair<String, String>>): IngestionReport {
        if (files.isEmpty()) return IngestionReport(0, 0, 0, 0, 0)

        var chunksAdded = 0
        var chunksSkipped = 0
        var chunksModified = 0
        var artifactsCompiled = 0
        val sectionsAdded = mutableMapOf<GovernanceSection, Int>()
        val sectionsTotal = mutableMapOf<GovernanceSection, Int>()

        for ((sourceFile, content) in files) {
            if (content.isBlank()) continue

            val newChunks = chunker.chunk(content, sourceFile)
            if (newChunks.isEmpty()) continue

            val ontologized = ontologizer.ontologize(newChunks)
            val existingChunks = repository.listChunks(Int.MAX_VALUE)
                .filter { it.chunk.sourceFile == sourceFile }

            for (chunk in ontologized) {
                val section = governanceOntologizer?.classify(chunk.chunk)
                val existing = existingChunks.firstOrNull {
                    it.chunk.sourceLines == chunk.chunk.sourceLines
                }
                if (existing != null) {
                    if (existing.chunk.checksum == chunk.chunk.checksum) {
                        chunksSkipped++
                        section?.let { sectionsTotal.merge(it, 1, Int::plus) }
                        continue
                    } else {
                        chunksModified++
                    }
                } else {
                    chunksAdded++
                    section?.let { sectionsAdded.merge(it, 1, Int::plus) }
                }
                repository.insertChunk(chunk)

                val artifact = compiler.compile(chunk)
                if (artifact != null) {
                    artifactsCompiled++
                }
                section?.let { sectionsTotal.merge(it, 1, Int::plus) }
            }
        }

        return IngestionReport(
            filesScanned = files.size,
            chunksAdded = chunksAdded,
            chunksSkipped = chunksSkipped,
            chunksModified = chunksModified,
            artifactsCompiled = artifactsCompiled,
            sectionsAdded = sectionsAdded,
            sectionsTotal = sectionsTotal
        )
    }
}
