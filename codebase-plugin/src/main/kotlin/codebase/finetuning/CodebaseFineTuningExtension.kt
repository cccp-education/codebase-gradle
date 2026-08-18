package codebase.finetuning

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Gradle DSL extension for the fine-tuning pipeline — EPIC FT-PIPELINE US-4.
 *
 * Pattern `CodebaseExpertExtension` / `CodebaseOcrExtension` — abstract class
 * with Gradle-managed `Property` / `ListProperty` fields. Defaults are backward
 * compatible and mirror [FineTuningConfig] / [FineTuningRequest] defaults.
 *
 * Sources of configuration (priority ascending):
 *  1. ENV vars (`CODEBASE_FINETUNING_*`) — via [FineTuningConfigMerger]
 *  2. `gradle.properties` (`codebase.finetuning.*`) — via [FineTuningConfigMerger]
 *  3. YAML file — via [FineTuningConfigMerger]
 *  4. CLI `-P` params — via [FineTuningConfigMerger]
 *  5. DSL block `fineTuning { ... }` — this extension (highest priority, set in build script)
 *
 * The [toConfig] / [toRequest] helpers bridge the Gradle properties back to the
 * pure domain objects so tasks can stay thin.
 */
abstract class CodebaseFineTuningExtension {

    abstract val baseModel: Property<String>
    abstract val dataset: ListProperty<String>
    abstract val outputModelName: Property<String>
    abstract val corpusRatio: Property<Double>
    abstract val maxIterations: Property<Int>
    abstract val epochs: Property<Int>
    abstract val learningRate: Property<Double>
    abstract val batchSize: Property<Int>
    abstract val corpusGlobs: ListProperty<String>
    abstract val validationThreshold: Property<Double>

    init {
        baseModel.convention("")
        dataset.convention(emptyList())
        outputModelName.convention("")
        corpusRatio.convention(0.10)
        maxIterations.convention(3)
        epochs.convention(3)
        learningRate.convention(2e-4)
        batchSize.convention(4)
        corpusGlobs.convention(emptyList())
        validationThreshold.convention(0.7)
    }

    /**
     * Maps the hyperparameter fields to a pure [FineTuningConfig].
     * Used by `fineTuneExpert` to build the [FineTuningState].
     */
    fun toConfig(): FineTuningConfig = FineTuningConfig(
        epochs = epochs.get(),
        learningRate = learningRate.get(),
        batchSize = batchSize.get(),
        corpusGlobs = corpusGlobs.get(),
        continualPreTrainingRatio = corpusRatio.get()
    )

    /**
     * Maps the request fields to a pure [FineTuningRequest].
     *
     * The [FineTuningRequest.dataset] is sourced from [dataset] when non-empty,
     * otherwise falls back to [corpusGlobs] (pattern `FineTuningConfig.corpusGlobs`).
     *
     * @throws IllegalArgumentException if `baseModel` is blank, `outputModelName`
     *         is blank, or both `dataset` and `corpusGlobs` are empty — delegated
     *         to [FineTuningRequest] invariants.
     */
    fun toRequest(): FineTuningRequest {
        val resolvedDataset = dataset.get().ifEmpty { corpusGlobs.get() }
        return FineTuningRequest(
            baseModel = baseModel.get(),
            dataset = resolvedDataset,
            outputModelName = outputModelName.get(),
            corpusRatio = corpusRatio.get()
        )
    }
}