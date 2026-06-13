package codebase.koog.agentic

import java.security.MessageDigest

class AgenticChunker {

    fun chunk(content: String, sourceFile: String): List<AgenticChunk> {
        if (content.isBlank()) return emptyList()

        val lines = content.lines()
        val chunks = mutableListOf<AgenticChunk>()

        var currentSection = ""
        var currentStartLine = 0
        val sectionBuffer = StringBuilder()

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1

            val isSectionHeader = (line.startsWith("== ") && !line.startsWith("=== ")) ||
                (line.startsWith("= ") && !line.startsWith("== "))

            if (isSectionHeader) {
                if (sectionBuffer.isNotBlank()) {
                    chunks.addAll(processSection(sectionBuffer.toString(), currentSection, currentStartLine, sourceFile))
                }
                currentSection = line.removePrefix("== ").removePrefix("= ").trim()
                currentStartLine = lineNumber
                sectionBuffer.clear()
            }
            sectionBuffer.appendLine(line)
        }

        if (sectionBuffer.isNotBlank()) {
            chunks.addAll(processSection(sectionBuffer.toString(), currentSection, currentStartLine, sourceFile))
        }

        if (chunks.isEmpty()) {
            chunks.addAll(processSection(content, "", 1, sourceFile))
        }

        return chunks
    }

    private fun processSection(sectionContent: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()

        val rules = extractRules(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(rules)

        val procedures = extractProcedures(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(procedures)

        val constraints = extractConstraints(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(constraints)

        val metadata = extractMetadata(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(metadata)

        val concepts = extractConcepts(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(concepts)

        val paragraphs = extractParagraphs(sectionContent, sectionTitle, startLine, sourceFile)
        chunks.addAll(paragraphs)

        if (chunks.isEmpty() && sectionContent.isNotBlank()) {
            chunks.add(buildChunk(
                content = sectionContent.trim(),
                sourceFile = sourceFile,
                sourceLines = "$startLine-${startLine + sectionContent.lines().size - 1}",
                chunkType = ChunkType.CONCEPT,
                verb = extractVerbFromContent(sectionContent),
                sectionTitle = sectionTitle
            ))
        }

        return chunks
    }

    private fun extractRules(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val rules = mutableListOf<AgenticChunk>()
        val rulePatterns = listOf(
            "INTERDICTION FORMELLE",
            "INTERDICTION",
            "NE DOIT JAMAIS",
            "NE JAMAIS",
            "OBLIGATOIRE",
            "INTERDIT"
        )

        val lines = content.lines()
        for ((i, line) in lines.withIndex()) {
            for (pattern in rulePatterns) {
                if (line.contains(pattern, ignoreCase = true)) {
                    val contextLines = buildContextBlock(lines, i, 2)
                    val lineRange = "${startLine + i}-${startLine + i + contextLines.lines().size - 1}"
                    val chunkContent = contextLines.trim()
                    rules.add(buildChunk(
                        content = chunkContent,
                        sourceFile = sourceFile,
                        sourceLines = lineRange,
                        chunkType = ChunkType.RULE,
                        verb = TaxonomyVerb.INTERDIRE,
                        sectionTitle = sectionTitle
                    ))
                    break
                }
            }
        }
        return rules
    }

    private fun extractProcedures(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()
        val lines = content.lines()

        val procedureStartIndices = mutableListOf<Int>()
        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.matches(Regex("^\\.\\s+.+")) || trimmed.matches(Regex("^\\[\\s*[x ]\\s*\\].+"))) {
                if (procedureStartIndices.isEmpty() || i - procedureStartIndices.last() > 3) {
                    procedureStartIndices.add(i)
                }
            }
        }

        for (startIdx in procedureStartIndices) {
            val blockLines = mutableListOf<String>()
            if (sectionTitle.isNotBlank()) {
                blockLines.add("== $sectionTitle")
            }
            var j = startIdx
            while (j < lines.size) {
                val trimmed = lines[j].trim()
                if (trimmed.matches(Regex("^\\.\\s+.+")) || trimmed.matches(Regex("^\\[\\s*[x ]\\s*\\].+"))) {
                    blockLines.add(lines[j])
                    j++
                } else if (blockLines.isNotEmpty() && trimmed.isEmpty()) {
                    break
                } else if (blockLines.isNotEmpty()) {
                    break
                } else {
                    j++
                }
            }
            if (blockLines.size > (if (sectionTitle.isNotBlank()) 1 else 0)) {
                val lineRange = "${startLine + startIdx}-${startLine + startIdx + blockLines.size - 1}"
                val chunkContent = blockLines.joinToString("\n").trim()
                val verb = if (chunkContent.contains("verifier", ignoreCase = true) ||
                    chunkContent.contains("check", ignoreCase = true) ||
                    chunkContent.contains("compile", ignoreCase = true) ||
                    chunkContent.contains("valider", ignoreCase = true)
                ) TaxonomyVerb.VALIDER else null
                chunks.add(buildChunk(
                    content = chunkContent,
                    sourceFile = sourceFile,
                    sourceLines = lineRange,
                    chunkType = ChunkType.PROCEDURE,
                    verb = verb,
                    sectionTitle = sectionTitle
                ))
            }
        }
        return chunks
    }

    private fun extractConstraints(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()
        val constraintPatterns = listOf(
            "Maximum",
            "max ",
            "limite",
            "Contexte leger",
            "1 fichier a la fois",
            "50k tokens",
            "~3000 lignes",
            "5 sessions de 20 minutes"
        )

        val lines = content.lines()
        for ((i, line) in lines.withIndex()) {
            for (pattern in constraintPatterns) {
                if (line.contains(pattern, ignoreCase = true)) {
                    val contextLines = buildContextBlock(lines, i, 1)
                    val lineRange = "${startLine + i}-${startLine + i + contextLines.lines().size - 1}"
                    chunks.add(buildChunk(
                        content = contextLines.trim(),
                        sourceFile = sourceFile,
                        sourceLines = lineRange,
                        chunkType = ChunkType.CONSTRAINT,
                        verb = TaxonomyVerb.VALIDER,
                        sectionTitle = sectionTitle
                    ))
                    break
                }
            }
        }
        return chunks
    }

    private fun extractMetadata(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()
        val lines = content.lines()

        val metadataLines = mutableListOf<Pair<Int, String>>()
        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(":") && trimmed.contains(":")) {
                metadataLines.add(i to line)
            }
            if (trimmed.startsWith("_") && trimmed.contains("mise a jour") && trimmed.endsWith("_")) {
                metadataLines.add(i to line)
            }
        }

        if (metadataLines.isNotEmpty()) {
            val blockLines = metadataLines.map { it.second }
            val firstIdx = metadataLines.first().first
            val lastIdx = metadataLines.last().first
            val lineRange = "${startLine + firstIdx}-${startLine + lastIdx}"
            chunks.add(buildChunk(
                content = blockLines.joinToString("\n").trim(),
                sourceFile = sourceFile,
                sourceLines = lineRange,
                chunkType = ChunkType.METADATA,
                verb = null,
                sectionTitle = sectionTitle
            ))
        }
        return chunks
    }

    private fun extractParagraphs(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()
        val lines = content.lines()

        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("=") || trimmed.startsWith(":") || trimmed.startsWith("_")) continue

            val isRule = trimmed.contains("INTERDICTION", ignoreCase = true) ||
                trimmed.contains("NE DOIT JAMAIS", ignoreCase = true) ||
                trimmed.contains("NE JAMAIS", ignoreCase = true) ||
                trimmed.contains("OBLIGATOIRE", ignoreCase = true) ||
                trimmed.contains("INTERDIT", ignoreCase = true)
            val isProcedure = trimmed.matches(Regex("^\\.\\s+.+")) || trimmed.matches(Regex("^\\[\\s*[x ]\\s*\\].+"))
            val isConstraint = trimmed.contains("Maximum", ignoreCase = true) ||
                trimmed.contains("limite", ignoreCase = true) ||
                trimmed.contains("50k tokens", ignoreCase = true) ||
                trimmed.contains("~3000 lignes", ignoreCase = true) ||
                trimmed.contains("1 fichier a la fois", ignoreCase = true) ||
                trimmed.contains("Contexte leger", ignoreCase = true)
            val isConceptBullet = trimmed.matches(Regex("^\\.\\s+.+"))

            if (isRule || isProcedure || isConstraint || isConceptBullet) continue

            val paraLines = mutableListOf<String>()
            if (sectionTitle.isNotBlank()) {
                paraLines.add("== $sectionTitle")
            }
            paraLines.add(line)

            val lineRange = "${startLine + i}-${startLine + i}"
            val chunkContent = paraLines.joinToString("\n").trim()

            chunks.add(buildChunk(
                content = chunkContent,
                sourceFile = sourceFile,
                sourceLines = lineRange,
                chunkType = ChunkType.CONCEPT,
                verb = extractVerbFromContent(chunkContent),
                sectionTitle = sectionTitle
            ))
        }

        return chunks
    }

    private fun extractConcepts(content: String, sectionTitle: String, startLine: Int, sourceFile: String): List<AgenticChunk> {
        val chunks = mutableListOf<AgenticChunk>()

        if (sectionTitle.isNotBlank()) {
            val conceptContent = buildSectionSummary(content, sectionTitle)
            chunks.add(buildChunk(
                content = conceptContent,
                sourceFile = sourceFile,
                sourceLines = "$startLine-${startLine + content.lines().size - 1}",
                chunkType = ChunkType.CONCEPT,
                verb = extractVerbFromContent(content),
                sectionTitle = sectionTitle
            ))
        }

        val lines = content.lines()
        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.startsWith(". ") && trimmed.length > 3) {
                val contextLines = buildContextBlock(lines, i, 1)
                val lineRange = "${startLine + i}-${startLine + i + contextLines.lines().size - 1}"
                chunks.add(buildChunk(
                    content = contextLines.trim(),
                    sourceFile = sourceFile,
                    sourceLines = lineRange,
                    chunkType = ChunkType.CONCEPT,
                    verb = extractVerbFromContent(contextLines),
                    sectionTitle = sectionTitle
                ))
            }
        }

        return chunks
    }

    private fun buildSectionSummary(content: String, sectionTitle: String): String {
        val lines = content.lines()
        val summaryLines = mutableListOf<String>()
        summaryLines.add("== $sectionTitle")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("==") && !trimmed.startsWith(":")) {
                summaryLines.add(line)
                if (summaryLines.size >= 5) break
            }
        }
        return summaryLines.joinToString("\n").trim()
    }

    private fun buildContextBlock(lines: List<String>, centerIndex: Int, radius: Int): String {
        val start = maxOf(0, centerIndex - radius)
        val end = minOf(lines.size, centerIndex + radius + 1)
        return lines.subList(start, end).joinToString("\n")
    }

    private fun extractVerbFromContent(content: String): TaxonomyVerb? {
        val lower = content.lowercase()
        return when {
            lower.contains("interdiction") || lower.contains("ne doit jamais") || lower.contains("ne jamais") -> TaxonomyVerb.INTERDIRE
            lower.contains("generate") || lower.contains("generer") || lower.contains("produit") -> TaxonomyVerb.GENERER
            lower.contains("collect") || lower.contains("importe") || lower.contains("acquisition") -> TaxonomyVerb.COLLECTER
            lower.contains("transform") || lower.contains("convertit") || lower.contains("conversion") -> TaxonomyVerb.TRANSFORMER
            lower.contains("deploy") || lower.contains("publie") || lower.contains("publication") -> TaxonomyVerb.DEPLOYER
            lower.contains("verifier") || lower.contains("valider") || lower.contains("check") || lower.contains("compile") -> TaxonomyVerb.VALIDER
            else -> null
        }
    }

    private fun extractDomain(sourceFile: String): String? {
        val domainPatterns = mapOf(
            "codebase" to "codebase",
            "planner" to "planner",
            "codex" to "codex",
            "training" to "training",
            "quizz" to "quizz",
            "runner" to "runner",
            "capsule" to "capsule",
            "hyperframes" to "hyperframes",
            "readme" to "readme",
            "document" to "document",
            "bakery" to "bakery",
            "slider" to "slider",
            "plantuml" to "plantuml",
            "graphify" to "graphify",
            "api-key-pool" to "api-key-pool",
            "workspace-bom" to "workspace-bom",
            "workspace" to "workspace",
            "waiter" to "waiter",
            "agile" to "agile",
            "ticket" to "ticket",
            "review" to "review",
            "flow" to "flow",
            "jhipster" to "jhipster",
            "magic-stick" to "magic-stick",
            "newpipe" to "newpipe",
            "notebook" to "notebook"
        )
        val lower = sourceFile.lowercase()
        for ((key, domain) in domainPatterns) {
            if (lower.contains(key)) return domain
        }
        return null
    }

    private fun extractDagLevel(content: String): DagLevel? {
        val match = Regex("\\b(N[0-4])\\b", RegexOption.IGNORE_CASE).find(content)
        return match?.value?.let { DagLevel.fromString(it) }
    }

    private fun extractCircle(content: String): Int? {
        val match = Regex("\\*?cercle\\*?\\s*(\\d)", RegexOption.IGNORE_CASE).find(content)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractWeight(content: String): Double {
        val lower = content.lowercase()
        return when {
            lower.contains("critique") || lower.contains("bloquant") || lower.contains("absolue") || lower.contains("formelle") -> 1.0
            lower.contains("important") || lower.contains("obligatoire") -> 0.8
            else -> 0.5
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildChunk(
        content: String,
        sourceFile: String,
        sourceLines: String,
        chunkType: ChunkType,
        verb: TaxonomyVerb?,
        sectionTitle: String
    ): AgenticChunk {
        val domain = extractDomain(sourceFile)
        val dagLevel = extractDagLevel(content)
        val circle = extractCircle(content)
        val weight = extractWeight(content)
        val checksum = sha256(content)
        val id = sha256("$sourceFile:$sourceLines:$content")

        return AgenticChunk(
            id = id,
            sourceFile = sourceFile,
            sourceLines = sourceLines,
            chunkType = chunkType,
            content = content,
            verb = verb,
            domain = domain,
            dagLevel = dagLevel,
            circle = circle,
            weight = weight,
            checksum = checksum
        )
    }
}
