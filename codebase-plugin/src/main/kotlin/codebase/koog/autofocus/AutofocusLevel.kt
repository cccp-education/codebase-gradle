package codebase.koog.autofocus

enum class AutofocusLevel(
    val tokenBudget: Int,
    val filePattern: String,
    val description: String
) {
    BIG_PICTURE(
        tokenBudget = 8000,
        filePattern = "**/*.adoc",
        description = "Vision globale : roadmap, EPICs, DAG, boroughs"
    ),
    ARCHITECTURE(
        tokenBudget = 4000,
        filePattern = "**/{build.gradle.kts,settings.gradle.kts,libs.versions.toml}",
        description = "Structure inter-boroughs : dépendances, contrats, API"
    ),
    MODULE(
        tokenBudget = 2000,
        filePattern = "**/*.kt",
        description = "Module/plugin : classes, interfaces, tests"
    ),
    IMPLEMENTATION(
        tokenBudget = 500,
        filePattern = "*.kt",
        description = "Code précis : une classe, une fonction, une ligne"
    );

    companion object {
        fun fromName(name: String): AutofocusLevel? =
            entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

        fun metadataByLevel(): Map<AutofocusLevel, LevelMetadata> = mapOf(
            BIG_PICTURE to LevelMetadata(
                contextSources = listOf(
                    "AGENTS.adoc", "INDEX.adoc", "PLAN_GLOBAL.adoc",
                    "GLOBAL_CODE_REVIEW.adoc", "PROMPT_REPRISE.adoc"
                ),
                summarization = "Résumé 50 tokens/borough → 550 tokens total"
            ),
            ARCHITECTURE to LevelMetadata(
                contextSources = listOf(
                    "build.gradle.kts", "metadata.json", "ROADMAP_*.adoc"
                ),
                summarization = "Signature des tâches Gradle + contrats DAG"
            ),
            MODULE to LevelMetadata(
                contextSources = listOf("*.kt", "*Test.kt", "*.feature"),
                summarization = "Classes publiques + leurs dépendances directes"
            ),
            IMPLEMENTATION to LevelMetadata(
                contextSources = listOf("fichier cible + stacktrace + 2-3 dépendants"),
                summarization = "Code brut, pas de résumé"
            )
        )
    }
}

data class LevelMetadata(
    val contextSources: List<String>,
    val summarization: String
)
