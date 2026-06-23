package codebase.koog.agentic

class AgenticCompiler {

    fun compile(chunk: OntologizedChunk): CompiledArtifact? {
        if (!shouldCompile(chunk)) return null
        val executable = compileExecutable(chunk)
        return executable.compiledArtifact
    }

    fun compileExecutable(chunk: OntologizedChunk): ExecutableArtifact {
        val c = chunk.chunk
        val artifactType = determineArtifactType(c.chunkType, c.verb)
        val description = buildDescription(c.content, c.chunkType, c.verb)
        val confidence = computeConfidence(chunk)
        val payload = buildPayload(c, artifactType, description)

        val compiled = CompiledArtifact(
            sourceChunkId = c.id,
            artifactType = artifactType,
            description = description,
            targetHint = c.domain,
            confidence = confidence,
            payload = payload
        )

        return ExecutableArtifact(compiled, payload)
    }

    private fun determineArtifactType(chunkType: ChunkType, verb: TaxonomyVerb?): ArtifactType {
        return when (chunkType) {
            ChunkType.RULE -> when (verb) {
                TaxonomyVerb.INTERDIRE -> ArtifactType.PRE_HOOK
                else -> ArtifactType.CI_GATE
            }
            ChunkType.PROCEDURE -> when (verb) {
                TaxonomyVerb.GENERER, TaxonomyVerb.DEPLOYER,
                TaxonomyVerb.TRANSFORMER, TaxonomyVerb.COLLECTER -> ArtifactType.GRADLE_TASK
                TaxonomyVerb.VALIDER -> ArtifactType.VALIDATION
                else -> ArtifactType.VALIDATION
            }
            ChunkType.CONSTRAINT -> ArtifactType.CONSTRAINT_CHECK
            ChunkType.CONCEPT -> when (verb) {
                TaxonomyVerb.INTERDIRE -> ArtifactType.PROMPT_TEMPLATE
                else -> ArtifactType.METADATA
            }
            ChunkType.METADATA -> ArtifactType.METADATA
        }
    }

    fun shouldCompile(chunk: OntologizedChunk): Boolean {
        return when (chunk.chunk.chunkType) {
            ChunkType.RULE -> true
            ChunkType.PROCEDURE -> true
            ChunkType.CONSTRAINT -> true
            ChunkType.CONCEPT -> true
            ChunkType.METADATA -> true
        }
    }

    private fun buildDescription(content: String, chunkType: ChunkType, verb: TaxonomyVerb?): String {
        val lines = content.lines()
        val firstLine = lines.firstOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("=") } ?: content
        val prefix = when (chunkType) {
            ChunkType.RULE -> "Rule"
            ChunkType.PROCEDURE -> "Procedure"
            ChunkType.CONSTRAINT -> "Constraint"
            ChunkType.CONCEPT -> "Concept"
            ChunkType.METADATA -> "Metadata"
        }
        val verbSuffix = if (verb != null) " (${verb.name})" else ""
        return "$prefix$verbSuffix: ${firstLine.trim().take(120)}"
    }

    private fun computeConfidence(chunk: OntologizedChunk): Double {
        val base = chunk.ontologyConfidence * 0.6 + chunk.chunk.weight * 0.4
        return base.coerceIn(0.0, 1.0)
    }

    private fun buildPayload(chunk: AgenticChunk, artifactType: ArtifactType, description: String): ArtifactPayload {
        return when (artifactType) {
            ArtifactType.PRE_HOOK -> ArtifactPayload.PreHookPayload(
                toolName = inferToolName(chunk.content),
                forbiddenPatterns = extractForbiddenPatterns(chunk.content),
                allowedPattern = extractAllowedPattern(chunk.content)
            )

            ArtifactType.POST_HOOK -> ArtifactPayload.PostHookPayload(
                toolName = inferToolName(chunk.content),
                forbiddenPatterns = extractForbiddenPatterns(chunk.content),
                allowedPattern = extractAllowedPattern(chunk.content)
            )

            ArtifactType.CI_GATE -> ArtifactPayload.ValidationPayload(
                checkDescription = description
            )

            ArtifactType.GRADLE_TASK -> ArtifactPayload.GradleTaskPayload(
                taskName = inferGradleTaskName(chunk.content),
                description = description
            )

            ArtifactType.VALIDATION -> ArtifactPayload.ValidationPayload(
                checkDescription = description
            )

            ArtifactType.METADATA -> {
                val (key, value) = extractMetadataPair(chunk.content, description)
                ArtifactPayload.MetadataPayload(
                    metadataKey = key,
                    metadataValue = value
                )
            }

            ArtifactType.PROMPT_TEMPLATE -> ArtifactPayload.PromptTemplatePayload(
                templateName = chunk.chunkType.name.lowercase(),
                promptText = chunk.content.take(500)
            )

            ArtifactType.CONSTRAINT_CHECK -> {
                val (maxTokens, maxLines) = extractConstraintBounds(chunk.content)
                ArtifactPayload.ConstraintPayload(
                    constraintDescription = description,
                    maxTokens = maxTokens,
                    maxLines = maxLines
                )
            }
        }
    }

    private fun inferToolName(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.contains("gradle") || lower.contains("gradlew") || lower.contains("task") -> "exec_gradle"
            lower.contains("git") || lower.contains("commit") || lower.contains("merge") ||
                lower.contains("push") || lower.contains("bash") || lower.contains("shell") ||
                lower.contains("rm ") || lower.contains("command") -> "exec_shell"
            else -> "exec_gradle"
        }
    }

    private fun extractForbiddenPatterns(content: String): List<String> {
        val lower = content.lowercase()
        val patterns = mutableListOf<String>()

        if (Regex("""git\s+push""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("push")
        if (Regex("""\bcommit\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("commit")
        if (Regex("""\bmerge\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("merge")
        if (Regex("""\bpublish\b|\bpublier\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("publish")
        if (Regex("""--force|force\s+push""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("--force")
        if (Regex("""\bdelete\b|\brm\s+-rf\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)) patterns.add("delete")

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

    private fun inferGradleTaskName(content: String): String {
        val lower = content.lowercase()
        return when {
            lower.contains("generate") || lower.contains("generer") -> "generateArtifact"
            lower.contains("deploy") || lower.contains("deployer") || lower.contains("publier") -> "deploySite"
            lower.contains("transform") || lower.contains("transformer") || lower.contains("convertir") -> "transformDocument"
            lower.contains("collect") || lower.contains("collecter") || lower.contains("import") -> "collectData"
            lower.contains("verify") || lower.contains("verifier") || lower.contains("compile") -> "verifyBuild"
            else -> "runGovernanceTask"
        }
    }

    private fun extractMetadataPair(content: String, description: String): Pair<String, String> {
        val attributePattern = Regex(""":([\w-]+):\s*(.+)""")
        val match = attributePattern.find(content)
        return if (match != null) {
            match.groupValues[1] to match.groupValues[2].trim()
        } else {
            "metadata" to description
        }
    }

    private fun extractConstraintBounds(content: String): Pair<Int?, Int?> {
        val tokenPattern = Regex("""(\d+)\s*k?\s*tokens?""", RegexOption.IGNORE_CASE)
        val linePattern = Regex("""(\d+)\s*lignes?""", RegexOption.IGNORE_CASE)
        val maxTokens = tokenPattern.find(content)?.groupValues?.get(1)?.toIntOrNull()?.let { it * 1000 }
        val maxLines = linePattern.find(content)?.groupValues?.get(1)?.toIntOrNull()
        return maxTokens to maxLines
    }
}
