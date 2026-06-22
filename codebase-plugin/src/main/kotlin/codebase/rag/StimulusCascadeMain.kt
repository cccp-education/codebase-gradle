package codebase.rag

import codebase.blog.BlogArticleExtractor
import codebase.blog.BlogArticleRenderer
import org.slf4j.LoggerFactory
import java.io.File

object StimulusCascadeMain {
    private val log = LoggerFactory.getLogger(StimulusCascadeMain::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val baseUrl = System.getenv("OLLAMA_BASE_URL") ?: "http://localhost:11437"
        val modelName = System.getenv("PERTINENCE_MODEL") ?: "gpt-oss:120b-cloud"
        val workspaceRoot = args.getOrNull(1) ?: System.getenv("WORKSPACE_ROOT") ?: ""
        val outputDirPath = args.getOrNull(2) ?: "build/stimulus-reports"
        val dryRun = args.any { it == "--dry-run" }
        val blogMode = args.any { it == "--blog" }

        val dumpContent: String = if (args.isNotEmpty()) {
            val dumpArg = args.firstOrNull { !it.startsWith("--") } ?: ""
            if (File(dumpArg).exists()) {
                File(dumpArg).readText(Charsets.UTF_8)
            } else if (dumpArg.isNotBlank()) {
                dumpArg
            } else {
                System.err.println("Usage: diluteBrainDump <brain-dump-text-or-file> [workspaceRoot] [outputDir] [--dry-run] [--blog]")
                System.err.println("       Passe le brain dump en argument (chaine de caracteres ou chemin de fichier)")
                return
            }
        } else {
            System.err.println("Usage: diluteBrainDump <brain-dump-text-or-file> [workspaceRoot] [outputDir] [--dry-run] [--blog]")
            System.err.println("       Passe le brain dump en argument (chaine de caracteres ou chemin de fichier)")
            return
        }

        StdoutFormatter.banner("EPIC 10 — STIMULUS Cascade : Brain Dump → Classification → Routing → Archivage")
        StdoutFormatter.ctx("Model         : $modelName ($baseUrl)")
        StdoutFormatter.ctx("Workspace     : ${workspaceRoot.ifBlank { "(cwd)" }}")
        StdoutFormatter.ctx("Output        : $outputDirPath")
        StdoutFormatter.ctx("Mode          : ${if (dryRun) "DRY RUN (pas d'archivage)" else "EXECUTION REELLE"}")
        if (blogMode) StdoutFormatter.ctx("Blog          : OUI — generation article de blog apres dilution")
        StdoutFormatter.ctx("Source size   : ${dumpContent.length} caracteres")
        StdoutFormatter.separator()

        val cascade = StimulusCascade(
            baseUrl = baseUrl,
            modelName = modelName,
            workspaceRoot = workspaceRoot,
            dryRun = dryRun
        )

        val report = cascade.execute(dumpContent)

        val outputDir = File(outputDirPath)
        outputDir.mkdirs()

        val jsonReport = cascade.exportCascadeJson(report)
        val jsonFile = File(outputDir, "stimulus-cascade.json")
        jsonFile.writeText(jsonReport)
        log.info("JSON report written: {}", jsonFile.absolutePath)

        val adocReport = cascade.exportCascadeAsciiDoc(report)
        val adocFile = File(outputDir, "stimulus-cascade.adoc")
        adocFile.writeText(adocReport)
        log.info("AsciiDoc report written: {}", adocFile.absolutePath)

        StdoutFormatter.separator()
        StdoutFormatter.ctx("Rapports generes :")
        StdoutFormatter.ctx("  JSON    : ${jsonFile.absolutePath}")
        StdoutFormatter.ctx("  AsciiDoc: ${adocFile.absolutePath}")
        StdoutFormatter.separator()

        if (report.visionCount > 0) {
            StdoutFormatter.result("Pipeline STIMULUS TERMINE — ${report.visionCount} sections VISION routees, ${report.opinionCount} sections OPINION confinees ✅")
        } else {
            StdoutFormatter.result("Pipeline STIMULUS TERMINE — ${report.sections.size} sections (0 VISION, ${report.opinionCount} OPINION confinees)")
        }

        if (blogMode) {
            generateBlogArticles(workspaceRoot)
        }
    }

    private fun generateBlogArticles(workspaceRoot: String) {
        StdoutFormatter.separator()
        StdoutFormatter.banner("EPIC 12 — Blog Narration Publique : Sessions → Articles JBake")

        val wsDir = if (workspaceRoot.isBlank()) File(".") else File(workspaceRoot)
        val foundryDir = File(wsDir, "foundry")
        val blogDir = File(wsDir, "office/sites/cheroliv/jbake/content/blog/2026")

        if (!foundryDir.exists()) {
            log.warn("[blog] foundry/ introuvable — extraction impossible")
            return
        }

        val extractor = BlogArticleExtractor()
        val articles = extractor.extract(foundryDir)

        if (articles.isEmpty()) {
            StdoutFormatter.ctx("Aucune session trouvee dans foundry/ — pas d'article genere")
            return
        }

        val nextNumber = findNextArticleNumber(blogDir)
        blogDir.mkdirs()

        var number = nextNumber
        for (article in articles) {
            val renderer = BlogArticleRenderer(articleNumber = number)
            renderer.render(article, blogDir)
            StdoutFormatter.ctx("  Article ${number.toString().padStart(4, '0')} : ${article.sessionNumber} — ${article.boroughName} : ${article.sessionTitle}")
            number++
        }

        StdoutFormatter.result("Blog — ${articles.size} articles generes dans ${blogDir.absolutePath}")
    }

    private fun findNextArticleNumber(blogDir: File): Int {
        val existing = blogDir.listFiles()
            ?.mapNotNull { f -> """^(\d{4})_""".toRegex().find(f.name)?.groupValues?.get(1)?.toIntOrNull() }
            ?.toList() ?: emptyList()
        return if (existing.isEmpty()) 127 else (existing.maxOrNull() ?: 127) + 1
    }
}
