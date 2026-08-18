package codebase.finetuning

import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertExposureTask
import codebase.koog.expert.ExpertRegistry
import codebase.koog.expert.ExpertRegistration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task `publishExpertToOllama` — EPIC FT-PIPELINE US-4.
 *
 * Closes the fabrication→exposition loop:
 *  1. Registers the fine-tuned model in the [ExpertRegistry] (domaine
 *     `codebase.koog.expert` — 13 fichiers existants) under the DSL-provided
 *     `domainName` / `domainLabel`.
 *  2. Delegates the manifest JSON generation to [ExpertExposureTask] — the
 *     manifest is consumed by slider/plantuml/bakery to route expert calls.
 *
 * The task is a thin Gradle adapter:
 *  - reads `outputModelName`, `domainName`, `domainLabel`, `baseUrl`,
 *    `anonymizeEndpoints`, `manifestOutput`,
 *  - validates them,
 *  - registers the [ExpertRegistration] in the [expertRegistry] (injectable
 *    for testing, defaults to a fresh [ExpertRegistry]),
 *  - builds an [ExpertExposureTask] and invokes `executeExposure()` to
 *    produce the manifest JSON.
 *
 * @see FineTuneExpertTask — precursor (produces the GGUF model).
 * @see ExpertExposureTask — delegated manifest generation.
 */
@DisableCachingByDefault(because = "Expert exposure — génération non-déterministe, dépend du registre runtime")
abstract class PublishExpertToOllamaTask : DefaultTask() {

    @get:Input
    abstract val outputModelName: Property<String>

    @get:Input
    abstract val domainName: Property<String>

    @get:Input
    @get:Optional
    abstract val domainLabel: Property<String>

    @get:Input
    abstract val baseUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val anonymizeEndpoints: Property<Boolean>

    @get:OutputFile
    abstract val manifestOutput: RegularFileProperty

    @get:Internal
    var expertRegistry: ExpertRegistry = ExpertRegistry()

    init {
        group = "finetuning"
        description = "Registers the fine-tuned expert in ExpertRegistry and generates the exposure manifest JSON"
        outputModelName.convention("")
        domainName.convention("")
        domainLabel.convention("")
        baseUrl.convention("http://localhost:11437")
        anonymizeEndpoints.convention(true)
    }

    @TaskAction
    fun executePublish() {
        val modelName = outputModelName.get().trim()
        val domain = domainName.get().trim()
        val label = domainLabel.get().trim().ifEmpty { domain }
        val endpoint = baseUrl.get()

        if (modelName.isBlank()) {
            throw GradleException("fineTuning.outputModelName must not be blank (publishExpertToOllama)")
        }
        if (domain.isBlank()) {
            throw GradleException("fineTuning.domainName must not be blank (publishExpertToOllama)")
        }

        val registration = ExpertRegistration(
            domain = ExpertDomain(name = domain, label = label),
            modelName = modelName,
            baseUrl = endpoint,
            timeoutSeconds = 120L
        )
        expertRegistry.register(registration)
        logger.lifecycle(
            "[publishExpertToOllama] registered expert: domain={}, model={}, baseUrl={}",
            domain, modelName, endpoint
        )

        val exposureTask = project.tasks.findByName("exposeExperts") as? ExpertExposureTask
            ?: project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        exposureTask.expertRegistry = expertRegistry
        exposureTask.outputFile.set(manifestOutput)
        exposureTask.anonymizeEndpoints.set(anonymizeEndpoints)
        exposureTask.executeExposure()

        logger.lifecycle(
            "[publishExpertToOllama] manifest written: {}",
            manifestOutput.get().asFile.absolutePath
        )
    }
}