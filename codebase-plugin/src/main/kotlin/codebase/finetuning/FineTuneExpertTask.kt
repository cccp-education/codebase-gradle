package codebase.finetuning

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path

/**
 * Gradle task `fineTuneExpert` — EPIC FT-PIPELINE US-4.
 *
 * Executes the [FineTuningPipeline] with the request built from the DSL
 * properties and writes a JSON/text report describing the outcome
 * ([FineTuningResult.Success] or [FineTuningResult.Failure]).
 *
 * The task is a thin Gradle adapter:
 *  - reads the DSL properties (`baseModel`, `dataset`, `outputModelName`,
 *    `corpusRatio`, `corpusGlobs` fallback),
 *  - validates them (delegated to [FineTuningRequest] invariants),
 *  - delegates the heavy lifting to the [FineTuningPipeline] port —
 *    injectable for testing ([FakeFineTuner]) or production
 *    ([OllamaFineTunerAdapter] wired with [ollamaBaseUrl] + [ggufOutputDir]).
 *
 * The pipeline never throws for an operational failure — it returns
 * [FineTuningResult.Failure] (degraded mode, economy of ink). The task
 * surfaces that as a FAILURE entry in the report, not as a Gradle build
 * failure (the caller can inspect the report and retry).
 *
 * @see PrepareFineTuningDatasetTask — precursor in the task chain.
 * @see PublishExpertToOllamaTask — successor (registers expert + manifest).
 */
@DisableCachingByDefault(because = "Fine-tuning — non-déterministe, dépend du registre Ollama runtime")
abstract class FineTuneExpertTask : DefaultTask() {

    @get:Input
    abstract val baseModel: Property<String>

    @get:Input
    @get:Optional
    abstract val dataset: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val corpusGlobs: ListProperty<String>

    @get:Input
    abstract val outputModelName: Property<String>

    @get:Input
    abstract val corpusRatio: Property<Double>

    @get:Input
    @get:Optional
    abstract val ollamaBaseUrl: Property<String>

    @get:OutputFile
    abstract val outputReport: RegularFileProperty

    @get:OutputDirectory
    abstract val ggufOutputDir: DirectoryProperty

    @get:Internal
    var pipeline: FineTuningPipeline? = null

    init {
        group = "finetuning"
        description = "Executes the fine-tuning pipeline (continual pre-training) and produces a GGUF model + report"
        baseModel.convention("")
        dataset.convention(emptyList())
        corpusGlobs.convention(emptyList())
        outputModelName.convention("")
        corpusRatio.convention(0.10)
        ollamaBaseUrl.convention("http://localhost:11437")
    }

    @TaskAction
    fun executeFineTune() {
        val resolvedBaseModel = baseModel.get().trim()
        val resolvedOutputName = outputModelName.get().trim()
        val resolvedDataset = dataset.get().ifEmpty { corpusGlobs.get() }
        val ratio = corpusRatio.get()

        if (resolvedBaseModel.isBlank()) {
            throw GradleException("fineTuning.baseModel must not be blank (fineTuneExpert)")
        }
        if (resolvedOutputName.isBlank()) {
            throw GradleException("fineTuning.outputModelName must not be blank (fineTuneExpert)")
        }
        if (resolvedDataset.isEmpty()) {
            throw GradleException(
                "fineTuning.dataset (or corpusGlobs fallback) must not be empty (fineTuneExpert)"
            )
        }
        require(ratio in 0.0..1.0) {
            "fineTuning.corpusRatio must be in [0.0, 1.0], got $ratio"
        }

        val request = FineTuningRequest(
            baseModel = resolvedBaseModel,
            dataset = resolvedDataset,
            outputModelName = resolvedOutputName,
            corpusRatio = ratio
        )

        val resolvedPipeline = pipeline ?: buildDefaultPipeline()
        val result = resolvedPipeline.fineTune(request)

        val report = buildReport(request, result)
        val reportFile = outputReport.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(report)
        logger.lifecycle(
            "[fineTuneExpert] {} — model={}, score={}, report={}",
            if (result is FineTuningResult.Success) "SUCCESS" else "FAILURE",
            resolvedOutputName,
            if (result is FineTuningResult.Success) result.validationScore else "n/a",
            reportFile.absolutePath
        )
    }

    private fun buildDefaultPipeline(): FineTuningPipeline {
        val ggufDir: Path = ggufOutputDir.get().asFile.toPath()
        Files.createDirectories(ggufDir)
        val baseUrl = ollamaBaseUrl.get().removeSuffix("/")
        val registryClient = OllamaHttpRegistryClient(baseUrl = baseUrl, timeoutSeconds = 120L)
        return OllamaFineTunerAdapter(registryClient = registryClient, ggufOutputDir = ggufDir)
    }

    private fun buildReport(request: FineTuningRequest, result: FineTuningResult): String = buildString {
        appendLine("# Fine-tuning report — ${request.outputModelName}")
        appendLine("# Generated by fineTuneExpert (EPIC FT-PIPELINE US-4)")
        appendLine()
        appendLine("baseModel: ${request.baseModel}")
        appendLine("outputModelName: ${request.outputModelName}")
        appendLine("corpusRatio: ${request.corpusRatio}")
        appendLine("dataset:")
        request.dataset.forEach { appendLine("  - $it") }
        appendLine()
        when (result) {
            is FineTuningResult.Success -> {
                appendLine("status: SUCCESS")
                appendLine("outputModelName: ${result.outputModelName}")
                appendLine("ggufPath: ${result.ggufPath}")
                appendLine("iterations: ${result.iterations}")
                appendLine("validationScore: ${result.validationScore}")
            }
            is FineTuningResult.Failure -> {
                appendLine("status: FAILURE")
                appendLine("reason: ${result.reason}")
                appendLine("originalDataset:")
                result.originalDataset.forEach { appendLine("  - $it") }
            }
        }
    }
}