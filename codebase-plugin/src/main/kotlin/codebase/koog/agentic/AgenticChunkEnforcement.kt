package codebase.koog.agentic

class AgenticChunkEnforcement {

    private val rules = mutableListOf<EnforcementRule>()

    fun registerFromCompiled(
        artifacts: List<CompiledArtifact>,
        chunks: List<OntologizedChunk>
    ): Int {
        var registered = 0
        val chunkMap = chunks.associateBy { it.chunk.id }

        for (artifact in artifacts) {
            if (artifact.artifactType != ArtifactType.PRE_HOOK) continue

            val chunk = chunkMap[artifact.sourceChunkId] ?: continue
            if (chunk.chunk.verb != TaxonomyVerb.INTERDIRE) continue

            val rules = extractRules(chunk, artifact)
            rules.forEach { rule ->
                this.rules.add(rule)
                registered++
            }
        }

        return registered
    }

    fun check(toolName: String, arguments: Map<String, String>): EnforcementResult {
        for (rule in rules) {
            if (rule.toolName != toolName) continue

            val taskArg = arguments["task"] ?: arguments["command"] ?: continue

            val forbiddenMatches = rule.forbiddenPattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(taskArg)
            if (!forbiddenMatches) continue

            if (rule.allowedPattern != null) {
                val allowedMatches = rule.allowedPattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(taskArg)
                if (allowedMatches) continue
            }

            return EnforcementResult.blocked(
                ruleId = rule.sourceChunkId,
                reason = rule.description
            )
        }

        return EnforcementResult.allowed()
    }

    fun ruleCount(): Int = rules.size

    fun clear() {
        rules.clear()
    }

    private fun extractRules(chunk: OntologizedChunk, artifact: CompiledArtifact): List<EnforcementRule> {
        val content = chunk.chunk.content
        val lower = content.lowercase()

        val forbiddenPatterns = extractForbiddenPatterns(content)
        if (forbiddenPatterns.isEmpty()) return emptyList()

        val allowedPattern = extractAllowedPattern(content)
        val toolName = inferToolName(content)

        return forbiddenPatterns.map { pattern ->
            EnforcementRule(
                sourceChunkId = artifact.sourceChunkId,
                verb = chunk.chunk.verb ?: TaxonomyVerb.INTERDIRE,
                forbiddenPattern = pattern,
                allowedPattern = allowedPattern,
                toolName = toolName,
                description = artifact.description
            )
        }
    }

    private fun extractForbiddenPatterns(content: String): List<String> {
        val lower = content.lowercase()
        val patterns = mutableListOf<String>()

        val gitPushPattern = Regex("""git\s+push""", RegexOption.IGNORE_CASE)
        if (gitPushPattern.containsMatchIn(lower)) patterns.add("push")

        val commitPattern = Regex("""\bcommit\b""", RegexOption.IGNORE_CASE)
        if (commitPattern.containsMatchIn(lower)) patterns.add("commit")

        val mergePattern = Regex("""\bmerge\b""", RegexOption.IGNORE_CASE)
        if (mergePattern.containsMatchIn(lower)) patterns.add("merge")

        val publishPattern = Regex("""\bpublish\b""", RegexOption.IGNORE_CASE)
        if (publishPattern.containsMatchIn(lower)) patterns.add("publish")

        val forcePattern = Regex("""--force|force\s+push""", RegexOption.IGNORE_CASE)
        if (forcePattern.containsMatchIn(lower)) patterns.add("--force")

        val deletePattern = Regex("""\bdelete\b|\brm\s+-rf\b""", RegexOption.IGNORE_CASE)
        if (deletePattern.containsMatchIn(lower)) patterns.add("delete")

        return patterns
    }

    private fun extractAllowedPattern(content: String): String? {
        val lower = content.lowercase()

        if (lower.contains("--dry-run") || lower.contains("dry run")) return "--dry-run"

        if (lower.contains("sauf") || lower.contains("except") || lower.contains("sans flag")) {
            val flagPattern = Regex("""--[\w-]+""")
            val match = flagPattern.find(content)
            if (match != null && match.value != "--force") return match.value
        }

        return null
    }

    private fun inferToolName(content: String): String {
        val lower = content.lowercase()

        if (lower.contains("gradle") || lower.contains("gradlew") ||
            lower.contains("./gradlew") || lower.contains("task")
        ) return "exec_gradle"

        val gitVerbs = listOf("git", "commit", "merge", "push", "rebase", "pull")
        if (gitVerbs.any { lower.contains(it) } ||
            lower.contains("bash") || lower.contains("shell") ||
            lower.contains("rm ") || lower.contains("command")
        ) return "exec_shell"

        return "exec_gradle"
    }
}
