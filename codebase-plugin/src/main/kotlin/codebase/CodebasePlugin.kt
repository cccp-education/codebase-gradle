package codebase

import codebase.blog.EndSessionBlogTask
import codebase.finetuning.CodebaseFineTuningExtension
import codebase.finetuning.FineTuneExpertTask
import codebase.finetuning.PrepareFineTuningDatasetTask
import codebase.finetuning.PublishExpertToOllamaTask
import codebase.koog.SessionProtocolDaemonTask
import codebase.koog.VibecodingTask
import codebase.koog.agentic.CodebaseGovernanceExtension
import codebase.koog.agentic.IngestGovernanceTask
import codebase.koog.expert.CodebaseExpertExtension
import codebase.koog.expert.ExpertExposureTask
import codebase.koog.tracking.DashboardTask
import codebase.ocr.CodebaseOcrExtension
import codebase.ocr.OcrIngestTask
import codebase.ocr.OcrTask
import codebase.quality.QualityGateTask
import codebase.rag.AssembleWorkspaceContextTask
import codebase.rag.CodebaseCompositeContextTask
import codebase.rag.PlanIntentionTask
import codebase.rag.PrepareContextTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class CodebasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ocrExt = project.extensions.create("codebaseOcr", CodebaseOcrExtension::class.java)
        ocrExt.ocrProvider.convention("gemini")
        ocrExt.geminiModel.convention("gemini-2.5-flash")
        ocrExt.ocrLanguage.convention("fr")
        ocrExt.outputFormat.convention("asciidoc")
        ocrExt.ocrEnabled.convention(false)
        ocrExt.maxTokens.convention(8192)
        ocrExt.inputDir.convention(project.layout.projectDirectory)

        val expertExt = project.extensions.create("codebaseExpert", CodebaseExpertExtension::class.java)
        expertExt.domains.convention(emptyList())
        expertExt.anonymizeEndpoints.convention(true)
        expertExt.outputFile.convention("build/experts/exposure-manifest.json")

        val governanceExt = project.extensions.create("codebaseGovernance", CodebaseGovernanceExtension::class.java)
        governanceExt.strictValidation.convention(false)
        governanceExt.outputEnabled.convention(true)
        governanceExt.reportFormat.convention("json")
        governanceExt.incremental.convention(false)
        governanceExt.chunkIncremental.convention(false)

        val fineTuningExt = project.extensions.create("fineTuning", CodebaseFineTuningExtension::class.java)

        project.tasks.register(
            "collectFromCodebase",
            PrepareContextTask::class.java
        ) {
            it.group = "collect"
            it.description = "Collects per-borough augmented context [REGLES_EAGER]/[CONTEXTE_RAG]/[RELATIONS_GRAPHIFY] into build/context/{name}.context.txt"
            it.workspaceRoot.set(project.rootDir)
            it.projectName.set(project.rootDir.name)
            it.ragQuestion.set(project.providers.gradleProperty("ragQuestion").orElse("architecture du workspace"))
            it.outputFile.set(project.layout.buildDirectory.file("context/${project.rootDir.name}.context.txt"))
        }

        project.tasks.register(
            "collectCompositeContext",
            AssembleWorkspaceContextTask::class.java
        ) {
            it.group = "collect"
            it.description = "Assembles all build/context/*.context.txt from foundry/ boroughs into a workspace-level composite"
            it.foundryDir.set(project.rootDir.parentFile.resolve("foundry/public"))
            it.outputFile.set(project.layout.buildDirectory.file("context/workspace-context.txt"))
        }

        project.tasks.register(
            "generatePlan",
            PlanIntentionTask::class.java
        ) {
            it.group = "generate"
            it.description = "Augmented Planning — classifies intention (pro/flash) then decomposes into EPICs/UserStories/Tasks using CompositeContext (EAGER+RAG+Graphify)"
            it.intention.set(project.providers.gradleProperty("intention").orElse(""))
            it.workspaceRoot.set(project.rootDir)
            it.ragQuestion.set(project.providers.gradleProperty("ragQuestion").orElse("architecture du workspace"))
            it.outputFile.set(project.layout.buildDirectory.file("plans/${project.rootDir.name}-plan.txt"))
        }

        project.tasks.register(
            "vibecode",
            VibecodingTask::class.java
        ) {
            it.group = "generate"
            it.description = "Vibecoding agent — koog autonomous loop (context → plan → execute → audit)"
            it.workspaceRoot.set(project.rootDir)
            it.intention.set(project.providers.gradleProperty("intention").orElse(""))
            it.dryRun.set(project.providers.gradleProperty("dryRun").map { it.toBoolean() }.orElse(false))
            it.maxActions.set(project.providers.gradleProperty("maxActions").map { it.toInt() }.orElse(10))
        }

        project.tasks.register(
            "sessionProtocolDaemon",
            SessionProtocolDaemonTask::class.java
        ) {
            it.group = "generate"
            it.description = "Session protocol daemon — stdin JSON-lines SessionPrompt, stdout SessionResponse, reuses Gradle daemon"
        }

        project.tasks.register(
            "ingestGovernance",
            IngestGovernanceTask::class.java
        ) {
            it.group = "generate"
            it.description = "Ingest governance EAGER files (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) into AgenticIngestor (in-memory stub, EPIC V-LOCAL pont Y)"
            it.workspaceRoot.set(project.rootDir)
            val strictProp = project.providers.gradleProperty("codebase.governance.strictValidation")
            val incrementalProp = project.providers.gradleProperty("codebase.governance.incremental")
            val chunkIncrementalProp = project.providers.gradleProperty("codebase.governance.chunkIncremental")
            it.governanceConfig.set(
                project.provider {
                    var config = governanceExt.toConfig()
                    strictProp.orElse("").get().takeIf { v -> v.isNotEmpty() }?.let { v ->
                        config = config.copy(strictValidation = v.toBoolean())
                    }
                    incrementalProp.orElse("").get().takeIf { v -> v.isNotEmpty() }?.let { v ->
                        config = config.copy(incremental = v.toBoolean())
                    }
                    chunkIncrementalProp.orElse("").get().takeIf { v -> v.isNotEmpty() }?.let { v ->
                        config = config.copy(chunkIncremental = v.toBoolean())
                    }
                    config
                }
            )
        }

        project.tasks.register(
            "vibecodingDashboard",
            DashboardTask::class.java
        ) {
            it.group = "tracking"
            it.description = "Dashboard vibecoding — résumé sessions, coûts tokens, filtres confidentialité"
        }

        project.tasks.register(
            "qualityGate",
            QualityGateTask::class.java
        ) {
            it.group = "validate"
            it.description = "Quality gate — validates expert LLM outputs (sentiment + off-topic + PII residual checks)"
            it.output.set(project.providers.gradleProperty("output").orElse(""))
            it.domain.set(project.providers.gradleProperty("domain").orElse("CDA"))
            it.minAcceptableScore.set(project.providers.gradleProperty("minScore").map { it.toDouble() }.orElse(0.60))
            it.enableSentimentCheck.set(project.providers.gradleProperty("enableSentiment").map { it.toBoolean() }.orElse(true))
            it.enableOffTopicCheck.set(project.providers.gradleProperty("enableOffTopic").map { it.toBoolean() }.orElse(true))
            it.enablePiiCheck.set(project.providers.gradleProperty("enablePii").map { it.toBoolean() }.orElse(true))
        }

        project.tasks.register(
            "endSessionBlog",
            EndSessionBlogTask::class.java
        ) { task ->
            task.group = "generate"
            task.description = "Blog narration publique — extrait sessions foundry/ et génère articles AsciiDoc JBake dans blog/"
            task.foundryDir.set(project.rootDir.parentFile.parentFile.resolve("foundry"))
            task.blogDir.set(project.rootDir.parentFile.parentFile.resolve(
                "office/sites/cheroliv/jbake/content/blog/2026"
            ))
            task.nextArticleNumber.set(
                project.providers.gradleProperty("nextArticleNumber").map { it.toInt() }.orElse(127)
            )
        }

        val trainingPluginDir = project.rootDir.parentFile.parentFile
            .resolve("private/training-gradle/training-plugin")
        project.tasks.register(
            "generateCompositeContext",
            CodebaseCompositeContextTask::class.java
        ) { task ->
            task.group = "generate"
            task.description = "Contexte composite N1+N2 : RagVectorStore (codebase.store) + training-gradle (AFNOR/REAC) → composite-context.json"
            task.query.set(project.providers.gradleProperty("query").orElse("architecture du workspace"))
            task.topK.set(project.providers.gradleProperty("topK").orElse("10"))
            task.trainingProjectDir.set(trainingPluginDir.absolutePath)
            task.outputFile.set(project.layout.buildDirectory.file("codebase/composite-context.json"))
        }

        project.tasks.register(
            "ocrDocument",
            OcrTask::class.java
        ) { task ->
            task.group = "collect"
            task.description = "OCR assisté IA — extrait le texte structuré d'un document scanné via Gemini Vision"
            val resolvedOcrProvider = project.providers.gradleProperty("ocrProvider").orElse(ocrExt.ocrProvider)
            task.ocrProvider.set(resolvedOcrProvider)
            // lifecycle { logger.lifecycle("[OCR Document Task] ocrProvider: {}", resolvedOcrProvider.getOrElse { "<not set>" }) }
            
            val resolvedOcrLanguage = project.providers.gradleProperty("ocrLanguage").orElse(ocrExt.ocrLanguage)
            task.ocrLanguage.set(resolvedOcrLanguage)
            //             lifecycle { logger.lifecycle("[OCR Document Task] ocrLanguage: {}", resolvedOcrLanguage.getOrElse { "<not set>" }) }
            
            val resolvedGeminiModel = project.providers.gradleProperty("geminiModel").orElse(ocrExt.geminiModel)
            task.geminiModel.set(resolvedGeminiModel)
            //             lifecycle { logger.lifecycle("[OCR Document Task] geminiModel: {}", resolvedGeminiModel.getOrElse { "<not set>" }) }
            
            val resolvedMaxTokens = project.providers.gradleProperty("maxTokens").map { it.toInt() }.orElse(ocrExt.maxTokens)
            task.maxTokens.set(resolvedMaxTokens)
            //             lifecycle { logger.lifecycle("[OCR Document Task] maxTokens: {}", resolvedMaxTokens.getOrElse { "<not set>" }) }
            
            val resolvedOutputFormat = project.providers.gradleProperty("outputFormat").orElse(ocrExt.outputFormat)
            task.outputFormat.set(resolvedOutputFormat)
            //             lifecycle { logger.lifecycle("[OCR Document Task] outputFormat: {}", resolvedOutputFormat.getOrElse { "<not set>" }) }
            
            // -PinputFile → CLI single-file mode
            val inputFileProp = project.providers.gradleProperty("inputFile")
            if (inputFileProp.isPresent) {
                task.inputFile.set(project.layout.projectDirectory.file(inputFileProp.get()))
                //                 lifecycle { logger.lifecycle("[OCR Document Task] inputFile: {}", task.inputFile.get().asFile.absolutePath) }
            }
            // -PinputDir → CLI batch mode
            val inputDirProp = project.providers.gradleProperty("inputDir")
            if (inputDirProp.isPresent) {
                task.inputDir.set(project.layout.projectDirectory.dir(inputDirProp.get()))
                //                 lifecycle { logger.lifecycle("[OCR Document Task] inputDir: {}", task.inputDir.get().asFile.absolutePath) }
            } else {
                task.inputDir.convention(ocrExt.inputDir)
                //                 lifecycle { logger.lifecycle("[OCR Document Task] inputDir (convention): {}", task.inputDir.get().asFile.absolutePath) }
            }
            // -PollamaBaseUrl → CLI ollama base URL
            val ollamaBaseUrlProp = project.providers.gradleProperty("ollamaBaseUrl")
            if (ollamaBaseUrlProp.isPresent) {
                task.ollamaBaseUrl.set(ollamaBaseUrlProp.get())
                //                 lifecycle { logger.lifecycle("[OCR Document Task] ollamaBaseUrl: {}", task.ollamaBaseUrl.get()) }
            }
            // -PollamaModel → CLI ollama model
            val ollamaModelProp = project.providers.gradleProperty("ollamaModel")
            if (ollamaModelProp.isPresent) {
                task.ollamaModel.set(ollamaModelProp.get())
                //                 lifecycle { logger.lifecycle("[OCR Document Task] ollamaModel: {}", task.ollamaModel.get()) }
            }
            // llm-config.yml → GeminiVisionProvider config
            val llmConfigFile = project.layout.projectDirectory.file("llm-config.yml").asFile
            if (llmConfigFile.exists()) {
                task.llmConfigFile = llmConfigFile
                //                 lifecycle { logger.lifecycle("[OCR Document Task] llmConfigFile: {}", llmConfigFile.absolutePath) }
            }
        }

        project.tasks.register(
            "ocrIngest",
            OcrIngestTask::class.java
        ) { task ->
            task.group = "collect"
            task.description = "Ingère les fichiers OCR dans pgvector (chunk → embedding ONNX → RAG)"
            task.ocrOutputDir.convention(project.layout.buildDirectory.dir("ocr"))
        }

        project.tasks.register(
            "exposeExperts",
            ExpertExposureTask::class.java
        ) { task ->
            task.group = "generate"
            task.description = "Expose les experts enregistrés via Ollama — génère un manifest JSON consommable par slider/plantuml/bakery"
            task.domains.set(
                project.providers.gradleProperty("domains")
                    .map { it.split(",").map { d -> d.trim() } }
                    .orElse(expertExt.domains)
            )
            task.anonymizeEndpoints.set(
                project.providers.gradleProperty("anonymizeEndpoints")
                    .map { it.toBoolean() }
                    .orElse(expertExt.anonymizeEndpoints)
            )
            task.outputFile.set(
                project.layout.buildDirectory.file(
                    project.providers.gradleProperty("outputFile")
                        .orElse(expertExt.outputFile)
                )
            )
        }

        // ─── EPIC FT-PIPELINE US-4 — fine-tuning tasks (prepare → fineTune → publish) ───
        val ftBaseModelProp = project.providers.gradleProperty("codebase.finetuning.baseModel")
        val ftDatasetProp = project.providers.gradleProperty("codebase.finetuning.dataset")
        val ftOutputNameProp = project.providers.gradleProperty("codebase.finetuning.outputModelName")
        val ftCorpusRatioProp = project.providers.gradleProperty("codebase.finetuning.corpusRatio")
        val ftMaxIterProp = project.providers.gradleProperty("codebase.finetuning.maxIterations")
        val ftEpochsProp = project.providers.gradleProperty("codebase.finetuning.epochs")
        val ftLearningRateProp = project.providers.gradleProperty("codebase.finetuning.learningRate")
        val ftBatchSizeProp = project.providers.gradleProperty("codebase.finetuning.batchSize")
        val ftCorpusGlobsProp = project.providers.gradleProperty("codebase.finetuning.corpusGlobs")
        val ftValidationThresholdProp = project.providers.gradleProperty("codebase.finetuning.validationThreshold")
        val ftDomainNameProp = project.providers.gradleProperty("codebase.finetuning.domainName")
        val ftDomainLabelProp = project.providers.gradleProperty("codebase.finetuning.domainLabel")
        val ftOllamaBaseUrlProp = project.providers.gradleProperty("codebase.finetuning.ollamaBaseUrl")

        project.tasks.register(
            "prepareFineTuningDataset",
            PrepareFineTuningDatasetTask::class.java
        ) { task ->
            task.group = "finetuning"
            task.description = "Prepares the fine-tuning dataset (Modelfile + corpus globs) for continual pre-training"
            task.baseModel.set(ftBaseModelProp.orElse(fineTuningExt.baseModel))
            task.dataset.set(
                ftDatasetProp.map { it.split(",").map { d -> d.trim() } }
                    .orElse(fineTuningExt.dataset)
            )
            task.corpusGlobs.set(
                ftCorpusGlobsProp.map { it.split(",").map { d -> d.trim() } }
                    .orElse(fineTuningExt.corpusGlobs)
            )
            task.outputModelName.set(ftOutputNameProp.orElse(fineTuningExt.outputModelName))
            task.corpusRatio.set(
                ftCorpusRatioProp.map { it.toDouble() }.orElse(fineTuningExt.corpusRatio)
            )
            task.outputFile.set(project.layout.buildDirectory.file("finetuning/dataset-prep.txt"))
        }

        project.tasks.register(
            "fineTuneExpert",
            FineTuneExpertTask::class.java
        ) { task ->
            task.group = "finetuning"
            task.description = "Executes the fine-tuning pipeline (continual pre-training) and produces a GGUF model + report"
            task.baseModel.set(ftBaseModelProp.orElse(fineTuningExt.baseModel))
            task.dataset.set(
                ftDatasetProp.map { it.split(",").map { d -> d.trim() } }
                    .orElse(fineTuningExt.dataset)
            )
            task.corpusGlobs.set(
                ftCorpusGlobsProp.map { it.split(",").map { d -> d.trim() } }
                    .orElse(fineTuningExt.corpusGlobs)
            )
            task.outputModelName.set(ftOutputNameProp.orElse(fineTuningExt.outputModelName))
            task.corpusRatio.set(
                ftCorpusRatioProp.map { it.toDouble() }.orElse(fineTuningExt.corpusRatio)
            )
            task.ollamaBaseUrl.set(ftOllamaBaseUrlProp.orElse("http://localhost:11437"))
            task.outputReport.set(project.layout.buildDirectory.file("finetuning/report.txt"))
            task.ggufOutputDir.set(project.layout.buildDirectory.dir("finetuning/gguf"))
            task.dependsOn("prepareFineTuningDataset")
        }

        project.tasks.register(
            "publishExpertToOllama",
            PublishExpertToOllamaTask::class.java
        ) { task ->
            task.group = "finetuning"
            task.description = "Registers the fine-tuned expert in ExpertRegistry and generates the exposure manifest JSON"
            task.outputModelName.set(ftOutputNameProp.orElse(fineTuningExt.outputModelName))
            task.domainName.set(ftDomainNameProp.orElse(""))
            task.domainLabel.set(ftDomainLabelProp.orElse(""))
            task.baseUrl.set(ftOllamaBaseUrlProp.orElse("http://localhost:11437"))
            task.anonymizeEndpoints.set(
                project.providers.gradleProperty("codebase.finetuning.anonymizeEndpoints")
                    .map { it.toBoolean() }
                    .orElse(true)
            )
            task.manifestOutput.set(project.layout.buildDirectory.file("finetuning/expert-manifest.json"))
            task.dependsOn("fineTuneExpert")
        }
    }
}
