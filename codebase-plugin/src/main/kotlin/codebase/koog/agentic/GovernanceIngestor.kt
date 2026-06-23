package codebase.koog.agentic

import java.io.File

/**
 * Orchestrateur DDD d'ingestion de gouvernance.
 *
 * Responsabilité unique : à partir d'un répertoire racine, scanner les fichiers
 * de gouvernance (.agents/, AGENT.adoc, etc.), les chunker/ontologiser, compiler
 * les artefacts exécutables et retourner le rapport + l'exécuteur de hooks.
 *
 * Cette classe est instanciable directement et ne dépend pas de Gradle.
 */
class GovernanceIngestor(
    private val chunkValidator: ChunkValidator = ChunkValidator()
) {

    fun ingest(workspaceRoot: File): IngestResult {
        val files = collectGovernanceFiles(workspaceRoot)

        val repository = InMemoryAgenticChunkRepository()
        val ingestor = AgenticIngestor(
            repository = repository,
            governanceOntologizer = GovernanceOntologizer(),
            chunkValidator = chunkValidator
        )

        val report = kotlinx.coroutines.runBlocking { ingestor.ingest(files) }
        val executor = if (report.executables.isNotEmpty()) AgenticExecutor(report.executables) else null

        return IngestResult(report, executor)
    }

    /**
     * Ingestion incrémentale : ne traite que les fichiers dont le [relativePath]
     * est dans [pathsToIngest]. Les autres fichiers sont ignorés pour ce tour.
     */
    fun ingestFiltered(workspaceRoot: File, pathsToIngest: Set<String>): IngestResult {
        val files = collectGovernanceFiles(workspaceRoot)
            .filter { (relativePath, _) -> relativePath in pathsToIngest }

        val repository = InMemoryAgenticChunkRepository()
        val ingestor = AgenticIngestor(
            repository = repository,
            governanceOntologizer = GovernanceOntologizer(),
            chunkValidator = chunkValidator
        )

        val report = kotlinx.coroutines.runBlocking { ingestor.ingest(files) }
        val executor = if (report.executables.isNotEmpty()) AgenticExecutor(report.executables) else null

        return IngestResult(report, executor)
    }

    private fun collectGovernanceFiles(projectDir: File): List<Pair<String, String>> {
        val scanAgent = ScanAgent()
        return scanAgent.scan(projectDir).map { it.relativePath to it.content }
    }

    data class IngestResult(
        val report: IngestionReport,
        val executor: AgenticExecutor?
    )
}
