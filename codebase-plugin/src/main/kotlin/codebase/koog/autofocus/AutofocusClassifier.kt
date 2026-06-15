package codebase.koog.autofocus

import codebase.koog.llm.LlmProvider

object AutofocusClassifier {

    private val patterns: Map<Regex, AutofocusLevel> = mapOf(
        Regex("""compilation\s+error""", RegexOption.IGNORE_CASE) to AutofocusLevel.IMPLEMENTATION,
        Regex("""error:.*line\s+\d+""", RegexOption.IGNORE_CASE) to AutofocusLevel.IMPLEMENTATION,
        Regex("""build\s+fail""", RegexOption.IGNORE_CASE) to AutofocusLevel.IMPLEMENTATION,
        Regex("""fix\s+(this|the)\s+(bug|error|test)""", RegexOption.IGNORE_CASE) to AutofocusLevel.IMPLEMENTATION,
        Regex("""\w+\.kt:\d+""") to AutofocusLevel.IMPLEMENTATION,

        Regex("""refactor.*module""", RegexOption.IGNORE_CASE) to AutofocusLevel.MODULE,
        Regex("""extract.*class""", RegexOption.IGNORE_CASE) to AutofocusLevel.MODULE,
        Regex("""implement.*task""", RegexOption.IGNORE_CASE) to AutofocusLevel.MODULE,
        Regex("""add.*test""", RegexOption.IGNORE_CASE) to AutofocusLevel.MODULE,
        Regex("""\w+\.gradle\.kts""") to AutofocusLevel.MODULE,

        Regex("""architecture""", RegexOption.IGNORE_CASE) to AutofocusLevel.ARCHITECTURE,
        Regex("""DAG|dependency.*graph""", RegexOption.IGNORE_CASE) to AutofocusLevel.ARCHITECTURE,
        Regex("""d.pendance entre""", RegexOption.IGNORE_CASE) to AutofocusLevel.ARCHITECTURE,
        Regex("""migrate.*groupId|rename.*package""", RegexOption.IGNORE_CASE) to AutofocusLevel.ARCHITECTURE,
        Regex("""contract|API.*change""", RegexOption.IGNORE_CASE) to AutofocusLevel.ARCHITECTURE,

        Regex("""MVP0|MVP\d""", RegexOption.IGNORE_CASE) to AutofocusLevel.BIG_PICTURE,
        Regex("""roadmap""", RegexOption.IGNORE_CASE) to AutofocusLevel.BIG_PICTURE,
        Regex("""EPIC\s+[A-Z]""") to AutofocusLevel.BIG_PICTURE,
        Regex("""g.n.rer.*formation""", RegexOption.IGNORE_CASE) to AutofocusLevel.BIG_PICTURE,
        Regex("""code\s+review""", RegexOption.IGNORE_CASE) to AutofocusLevel.BIG_PICTURE,
        Regex("""session\s+\d+""", RegexOption.IGNORE_CASE) to AutofocusLevel.BIG_PICTURE
    )

    fun classifySync(intention: String, buildOutput: String? = null): AutofocusLevel? {
        val combinedInput = "$intention ${buildOutput ?: ""}"
        for ((pattern, level) in patterns) {
            if (pattern.containsMatchIn(combinedInput)) return level
        }
        return null
    }

    suspend fun classify(
        intention: String,
        buildOutput: String? = null,
        llmProvider: LlmProvider
    ): AutofocusLevel {
        classifySync(intention, buildOutput)?.let { return it }

        val prompt = """
            Given this user intention, what is the required context level?
            Options: big-picture (roadmap/EPICs/vision), architecture (module dependencies/contracts),
            module (a single plugin/package), implementation (specific file/line/error).

            Intention: "$intention"
            ${if (buildOutput != null) "Build output: \"$buildOutput\"" else ""}

            Answer with exactly one word: big-picture, architecture, module, or implementation.
        """.trimIndent()
        val response = llmProvider.call(prompt).trim().lowercase()
        return when {
            response.contains("big-picture") -> AutofocusLevel.BIG_PICTURE
            response.contains("architecture") -> AutofocusLevel.ARCHITECTURE
            response.contains("implementation") -> AutofocusLevel.IMPLEMENTATION
            else -> AutofocusLevel.MODULE
        }
    }
}
