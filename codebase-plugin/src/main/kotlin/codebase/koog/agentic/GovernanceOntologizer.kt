package codebase.koog.agentic

enum class GovernanceSection {
    RULES_ABSOLUES,
    ETAT_EPICS,
    BACKLOG_ITEMS,
    HISTORIQUE,
    COVERAGE,
    MISSION,
    UNKNOWN
}

class GovernanceOntologizer {

    fun classify(chunk: AgenticChunk): GovernanceSection {
        val filename = chunk.sourceFile.substringAfterLast('/')
        return when (filename) {
            "AGENT.adoc" -> GovernanceSection.RULES_ABSOLUES
            "INDEX.adoc" -> GovernanceSection.ETAT_EPICS
            "BACKLOG.adoc" -> GovernanceSection.BACKLOG_ITEMS
            "SESSIONS_HISTORY.adoc" -> GovernanceSection.HISTORIQUE
            "TEST_COVERAGE_ANALYSIS.adoc" -> GovernanceSection.COVERAGE
            "PROMPT_REPRISE.adoc" -> GovernanceSection.MISSION
            else -> GovernanceSection.UNKNOWN
        }
    }
}
