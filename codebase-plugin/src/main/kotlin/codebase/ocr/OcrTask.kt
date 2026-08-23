package codebase.ocr

import codebase.koog.llm.GeminiVisionProvider
import codebase.koog.llm.OllamaOcrProvider
import codebase.koog.llm.VisionProvider
import codebase.koog.llm.pool.GeminiKeyPool
import codebase.koog.llm.pool.GeminiPoolFactory
import codebase.rag.GeminiConfig
import codebase.rag.LlmConfig
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Tâche Gradle d'OCR assisté IA (Gemini Vision).
 *
 * Pattern : `abstract class DefaultTask` + `@Option` + `@Input`/`@Output` —
 * aligné sur QualityGateTask, VibecodingTask.
 *
 * Usage CLI :
 * ```
 * ./gradlew ocrDocument -PinputFile=/tmp/scan.pdf -PocrLanguage=en
 * ./gradlew ocrDocument -PinputDir=/tmp/scans/   # mode batch
 * ```
 *
 * Usage DSL :
 * ```
 * codebaseOcr {
 *     ocrProvider = "gemini"
 *     geminiApiKeys = listOf(System.getenv("GEMINI_API_KEY_1") ?: "")
 * }
 * ```
 *
 * Injection OcrEngine : en test → FakeOcrEngine, en production → GeminiVisionEngine.
 */
@DisableCachingByDefault(because = "OCR IA — appel LLM non-déterministe, non-cacheable")
abstract class OcrTask : DefaultTask() {

    /**
     * Moteur OCR injectable.
     * En test : `FakeOcrEngine`. En production : `GeminiVisionEngine` (OCR-2).
     * `@get:Internal` car n'est pas un paramètre de build, mais une dépendance d'exécution.
     */
    @get:Internal
    var ocrEngine: OcrEngine = NoOpOcrEngine()

    @get:Internal
    var geminiVisionProvider: VisionProvider? = null

    @get:Internal
    var ollamaOcrProvider: VisionProvider? = null

    /**
     * Clés API Gemini injectables (DSL `geminiApiKeys` ou test).
     * Si non vide, un [GeminiKeyPool] est construit et injecté dans [GeminiVisionProvider]
     * pour la rotation automatique sur HTTP 429 (OCR-1).
     */
    @get:Internal
    var geminiApiKeys: List<String> = emptyList()

    /**
     * Fichier YAML de config LLM (pattern keyRef/envVar, jamais de clé en dur).
     * S'il existe (injecté par CodebasePlugin), les paramètres `geminiModel`,
     * `maxTokens`, `ocrLanguage` sont lus depuis `llm-config.yml` → `GeminiConfig`.
     * Sinon, les valeurs DSL/CLI/convention s'appliquent.
     */
    @get:Internal
    var llmConfigFile: java.io.File? = null

    @get:Internal
    val metricsCollector: MutableList<OcrMetrics> = mutableListOf()

    @get:Input
    @get:Optional
    @get:Option(option = "ocrProvider", description = "Fournisseur IA : gemini, ollama, ou gemini+ollama (fallback)")
    abstract val ocrProvider: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "ollamaBaseUrl", description = "URL de base Ollama (défaut : http://localhost:11437)")
    abstract val ollamaBaseUrl: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "ollamaModel", description = "Modèle Ollama vision (défaut : gpt-oss:120b-cloud)")
    abstract val ollamaModel: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:Option(option = "inputFile", description = "Fichier à OCR-iser (mode single-file)")
    abstract val inputFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:Option(option = "inputDir", description = "Répertoire contenant les documents à OCR-iser (mode batch)")
    abstract val inputDir: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "ocrLanguage", description = "Langue source : fr, en, auto")
    abstract val ocrLanguage: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "geminiModel", description = "Modèle Gemini : gemini-2.5-flash, gemini-2.5-pro")
    abstract val geminiModel: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "maxTokens", description = "Nombre maximum de tokens pour la requête")
    abstract val maxTokens: Property<Int>

    @get:OutputFile
    @get:Optional
    @get:Option(option = "outputFile", description = "Fichier de sortie (défaut : build/ocr/{filename}.adoc)")
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Optional
    @get:Option(option = "outputFormat", description = "Format de sortie : asciidoc, markdown, text")
    abstract val outputFormat: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "anonymize", description = "Anonymiser le texte extrait (emails, téléphones, clés API, IBAN, SSN)")
    abstract val anonymizeOutput: Property<Boolean>

    init {
        group = "collect"
        description = "OCR assisté IA — extrait le texte structuré d'un document scanné via Gemini Vision ou Ollama Vision"
        ocrProvider.convention("gemini")
        ocrLanguage.convention("fr")
        geminiModel.convention("gemini-2.5-flash")
        ollamaBaseUrl.convention("http://localhost:11437")
        ollamaModel.convention("gpt-oss:120b-cloud")
        maxTokens.convention(8192)
        outputFormat.convention("asciidoc")
        anonymizeOutput.convention(false)
    }

    @TaskAction
    fun executeOcr() {
        val provider = ocrProvider.orNull ?: "gemini"

        val geminiConfig = resolveGeminiConfig()
        if (geminiConfig != null) {
            logger.lifecycle("[OCR] llm-config.yml chargé : modèle={}", geminiConfig.resolveModel())
        }

        val lang = ocrLanguage.orNull ?: "fr"
        val model = geminiModel.orNull
            ?: geminiConfig?.resolveModel()
            ?: "gemini-2.5-flash"
        val tokens = maxTokens.orNull ?: 8192
        val format = outputFormat.orNull ?: "asciidoc"
        val ollamaUrl = ollamaBaseUrl.orNull ?: "http://localhost:11437"
        val ollamaVisionModel = ollamaModel.orNull ?: "gpt-oss:120b-cloud"

        val files = resolveInputFiles()
        if (files.isEmpty()) {
            throw IllegalArgumentException(
                "Aucun fichier d'entrée spécifié. Utilisez -PinputFile=/path/to/file " +
                "ou -PinputDir=/path/to/dir pour le mode batch"
            )
        }

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        outputDir.mkdirs()

        val ext = when (format) {
            "markdown" -> ".md"
            "text" -> ".txt"
            else -> ".adoc"
        }

        logger.lifecycle(
            "[OCR] Démarrage batch : {} fichier(s), langue={}, fournisseur={}, modèle={}, maxTokens={}",
            files.size, lang, provider, model, tokens
        )

        val totalFiles = files.size
        for (file in files) {
            processSingleFile(file, lang, model, tokens, provider, ollamaUrl, ollamaVisionModel, outputDir, ext, totalFiles)
        }

        logger.lifecycle("[OCR] Batch terminé : {} fichier(s) traités", files.size)

        if (metricsCollector.isNotEmpty()) {
            val report = OcrMetricsReport.generateAsciiDoc(metricsCollector.toList())
            val reportDir = project.layout.buildDirectory.dir("reports/ocr").get().asFile
            reportDir.mkdirs()
            val reportFile = File(reportDir, "ocr-metrics.adoc")
            reportFile.writeText(report, Charsets.UTF_8)
            logger.lifecycle("[OCR] Rapport métriques écrit dans : {}", reportFile.absolutePath)
        }
    }

    private fun resolveInputFiles(): List<File> {
        if (inputFile.isPresent) {
            val file = inputFile.get().asFile
            if (!file.exists()) {
                throw IllegalArgumentException("Fichier d'entrée introuvable : ${file.absolutePath}")
            }
            return listOf(file)
        }
        if (inputDir.isPresent) {
            val dir = inputDir.get().asFile
            if (!dir.exists() || !dir.isDirectory) {
                throw IllegalArgumentException("Répertoire d'entrée introuvable : ${dir.absolutePath}")
            }
            return dir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
        }
        return emptyList()
    }

    private fun processSingleFile(
        file: File,
        lang: String,
        model: String,
        tokens: Int,
        provider: String,
        ollamaUrl: String,
        ollamaVisionModel: String,
        outputDir: File,
        ext: String,
        totalFiles: Int
    ) {
        logger.lifecycle("[OCR] Traitement : {}", file.name)

        val isImage = isImageFile(file)
        val mimeType = detectMimeType(file.extension)

        val startTime = System.currentTimeMillis()
        val result = if (isImage) {
            executeImageOcr(file, mimeType, lang, model, tokens, provider, ollamaUrl, ollamaVisionModel)
        } else {
            executeTextOcr(file, lang, model, tokens)
        }
        val ocrDuration = System.currentTimeMillis() - startTime

        val effectiveModel = if (provider == "ollama") ollamaVisionModel else model

        val (finalResult, replacements, categories) = if (anonymizeOutput.orNull == true) {
            val anonymized = TextAnonymizer.anonymize(result)
            val replaced = TextAnonymizer.countReplacements(result, anonymized)
            val cats = TextAnonymizer.detectedCategories(result)
            logger.lifecycle("[OCR] Anonymisation : {} remplacement(s), catégories={}", replaced, cats)
            Triple(anonymized, replaced, cats)
        } else Triple(result, 0, emptyList<String>())

        val baseName = file.nameWithoutExtension
        val outputPath = if (outputFile.isPresent && totalFiles == 1) {
            outputFile.get().asFile
        } else {
            File(outputDir, "${baseName}_ocr$ext")
        }

        outputPath.writeText(finalResult, Charsets.UTF_8)
        logger.lifecycle("[OCR] Résultat écrit dans : {}", outputPath.absolutePath)

        val metrics = OcrMetricsCalculator.buildMetrics(
            fileName = file.name,
            fileSizeBytes = file.length(),
            isImage = isImage,
            provider = provider,
            model = effectiveModel,
            language = lang,
            ocrDurationMs = ocrDuration,
            outputLengthChars = finalResult.length,
            anonymizationReplacements = replacements,
            anonymizationCategories = categories
        )
        metricsCollector.add(metrics)
        logger.lifecycle(
            "[OCR] Métriques {} : durée={}, coût={}",
            file.name,
            OcrMetricsCalculator.formatDurationMs(ocrDuration),
            "${"%.6f".format(java.util.Locale.US, metrics.estimatedCostUsd)} USD"
        )
    }

    /**
     * Charge la configuration Gemini depuis llm-config.yml (s'il existe).
     * Le fichier YAML est optionnel : s'il n'est pas fourni ou absent, retourne null.
     */
    private fun resolveGeminiConfig(): GeminiConfig? {
        val configFile = llmConfigFile ?: return null
        if (!configFile.exists()) {
            logger.debug("[OCR] llm-config.yml absent, utilisation des valeurs DSL/CLI/convention")
            return null
        }
        return try {
            LlmConfig.fromYaml(configFile.readText(Charsets.UTF_8)).ai.gemini
        } catch (e: Exception) {
            logger.warn("[OCR] Erreur parsing llm-config.yml : {}, fallback DSL/CLI/convention", e.message)
            null
        }
    }

    private fun executeImageOcr(
        file: File,
        mimeType: String,
        language: String,
        model: String,
        maxTokens: Int,
        provider: String,
        ollamaUrl: String,
        ollamaVisionModel: String
    ): String {
        logger.lifecycle("[OCR] Mode image détecté : mimeType={}, provider={}", mimeType, provider)

        val imageBytes = file.readBytes()

        return when (provider) {
            "ollama" -> {
                val ollamaProvider = ollamaOcrProvider
                    ?: OllamaOcrProvider(baseUrl = ollamaUrl, model = ollamaVisionModel)
                runBlocking {
                    ollamaProvider.processImage(imageBytes, mimeType, language, ollamaVisionModel, maxTokens)
                }
            }
            "gemini+ollama" -> {
                val geminiProvider = geminiVisionProvider ?: resolveGeminiProvider(model)
                try {
                    runBlocking {
                        geminiProvider.processImage(imageBytes, mimeType, language, model, maxTokens)
                    }
                } catch (e: Exception) {
                    logger.warn("[OCR] Gemini failed: {} — fallback to Ollama", e.message)
                    try {
                        val ollamaProvider = ollamaOcrProvider
                            ?: OllamaOcrProvider(baseUrl = ollamaUrl, model = ollamaVisionModel)
                        runBlocking {
                            ollamaProvider.processImage(imageBytes, mimeType, language, ollamaVisionModel, maxTokens)
                        }
                    } catch (e2: Exception) {
                        logger.warn("[OCR] Ollama failed: {} — no software fallback in codebase", e2.message)
                        throw GradleException(AI_ONLY_ERROR_MESSAGE)
                    }
                }
            }
            "tesseract" -> {
                throw GradleException(AI_ONLY_ERROR_MESSAGE)
            }
            else -> {
                val geminiProvider = geminiVisionProvider ?: resolveGeminiProvider(model)
                runBlocking {
                    geminiProvider.processImage(imageBytes, mimeType, language, model, maxTokens)
                }
            }
        }
    }

    /**
     * Construit un [GeminiVisionProvider] avec un [GeminiKeyPool] si des clés sont disponibles
     * (DSL [geminiApiKeys] ou env vars `GEMINI_API_KEY_1..N`).
     * Si aucune clé n'est configurée, fallback sur [GeminiConfig] (single key).
     */
    private fun resolveGeminiProvider(model: String): GeminiVisionProvider {
        val pool = resolveGeminiPool(model)
        return if (pool != null && pool.size() > 0) {
            logger.lifecycle("[OCR] GeminiKeyPool construit : {} clé(s), rotation activée", pool.size())
            GeminiVisionProvider(keyPool = pool)
        } else {
            logger.lifecycle("[OCR] Aucune clé Gemini pool — fallback GeminiConfig single key")
            GeminiVisionProvider()
        }
    }

    private fun resolveGeminiPool(model: String): GeminiKeyPool? {
        if (geminiApiKeys.isNotEmpty()) {
            return GeminiPoolFactory.fromKeys(geminiApiKeys, model = model)
        }
        val envPool = GeminiPoolFactory.fromEnvVars(System.getenv(), model = model)
        return if (envPool.size() > 0) envPool else null
    }

    private fun executeTextOcr(
        file: File,
        language: String,
        model: String,
        maxTokens: Int
    ): String {
        logger.lifecycle("[OCR] Mode texte détecté")
        val content = file.readText(Charsets.UTF_8)
        return ocrEngine.process(content, language, model, maxTokens)
    }

    companion object {
        /**
         * Boundary rule (EPIC CDX-OCR-BOUNDARY): software OCR (Tesseract) is
         * actioned by codex (Brooklyn) via `collectOcr`. The codebase task is
         * AI-only (Gemini/Ollama vision).
         */
        const val AI_ONLY_ERROR_MESSAGE =
            "OCR provider 'tesseract' (software OCR) is not actioned by codebase — " +
                "use the codex plugin's collectOcr task instead (EPIC CDX-OCR-BOUNDARY)"

        private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "tiff")

        private val MIME_MAP = mapOf(
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "bmp" to "image/bmp",
            "tiff" to "image/tiff"
        )

        fun isImageFile(file: File): Boolean =
            file.extension.lowercase() in IMAGE_EXTENSIONS

        fun detectMimeType(extension: String): String =
            MIME_MAP[extension.lowercase()] ?: "application/octet-stream"
    }
}
