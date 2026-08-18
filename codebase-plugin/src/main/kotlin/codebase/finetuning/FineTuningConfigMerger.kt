package codebase.finetuning

import java.io.File

/**
 * Merges fine-tuning configuration from four sources with priority:
 * ENV vars < gradle.properties < YAML file < CLI -P params.
 *
 * Pattern aligné sur `CapsuleConfigMerger` (capsule) / `ValidationConfig`.
 * Each higher-priority source overrides the same key from lower-priority sources.
 */
object FineTuningConfigMerger {

    /**
     * Merges configuration from all four sources.
     *
     * @param projectDir The project directory (where gradle.properties lives)
     * @param yamlConfig The configuration loaded from the YAML file
     * @param cliParams  CLI -P params as a flat map (e.g. "epochs" -> "5")
     * @param yamlLoaded Whether the YAML file was actually found and loaded.
     *                   When false, YAML values are ignored and props/ENV take precedence.
     * @return The merged FineTuningConfig with all sources resolved
     */
    fun merge(
        projectDir: File,
        yamlConfig: FineTuningConfig,
        cliParams: Map<String, Any?>,
        yamlLoaded: Boolean = true
    ): FineTuningConfig {
        val propertiesConfig = loadFromGradleProperties(projectDir)
        val envConfig = loadFromEnvironment()

        val yaml: FineTuningConfig? = if (yamlLoaded) yamlConfig else null

        return FineTuningConfig(
            epochs = mergeInt(cliParams, "epochs", yaml?.epochs, propertiesConfig.epochs),
            learningRate = mergeDouble(cliParams, "learningRate", yaml?.learningRate, propertiesConfig.learningRate),
            batchSize = mergeInt(cliParams, "batchSize", yaml?.batchSize, propertiesConfig.batchSize),
            corpusGlobs = mergeStrList(cliParams, "corpusGlobs", yaml?.corpusGlobs, propertiesConfig.corpusGlobs, envConfig.corpusGlobs),
            continualPreTrainingRatio = mergeDouble(
                cliParams, "continualPreTrainingRatio",
                yaml?.continualPreTrainingRatio, propertiesConfig.continualPreTrainingRatio
            )
        )
    }

    /**
     * Loads configuration from gradle.properties in the project directory.
     * Only reads properties prefixed with "codebase.finetuning.".
     */
    internal fun loadFromGradleProperties(projectDir: File): FineTuningConfig {
        val props = mutableMapOf<String, String>()

        val propertiesFile = File(projectDir, "gradle.properties")
        if (propertiesFile.exists()) {
            propertiesFile.reader().useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("codebase.finetuning.") && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            props[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
            }
        }

        return FineTuningConfig(
            epochs = props["codebase.finetuning.epochs"]?.toIntOrNull() ?: 3,
            learningRate = props["codebase.finetuning.learningRate"]?.toDoubleOrNull() ?: 2e-4,
            batchSize = props["codebase.finetuning.batchSize"]?.toIntOrNull() ?: 4,
            corpusGlobs = props["codebase.finetuning.corpusGlobs"]?.let { splitCommaList(it) } ?: emptyList(),
            continualPreTrainingRatio = props["codebase.finetuning.continualPreTrainingRatio"]?.toDoubleOrNull() ?: 0.10
        )
    }

    /**
     * Loads configuration from environment variables prefixed with CODEBASE_FINETUNING_.
     *
     * Convention: CODEBASE_FINETUNING_EPOCHS → epochs,
     * CODEBASE_FINETUNING_LEARNING_RATE → learningRate, etc.
     */
    internal fun loadFromEnvironment(): FineTuningConfig {
        val env = System.getenv()

        return FineTuningConfig(
            epochs = env["CODEBASE_FINETUNING_EPOCHS"]?.toIntOrNull() ?: 3,
            learningRate = env["CODEBASE_FINETUNING_LEARNING_RATE"]?.toDoubleOrNull() ?: 2e-4,
            batchSize = env["CODEBASE_FINETUNING_BATCH_SIZE"]?.toIntOrNull() ?: 4,
            corpusGlobs = env["CODEBASE_FINETUNING_CORPUS_GLOBS"]?.let { splitCommaList(it) } ?: emptyList(),
            continualPreTrainingRatio = env["CODEBASE_FINETUNING_CONTINUAL_PRE_TRAINING_RATIO"]?.toDoubleOrNull() ?: 0.10
        )
    }

    // ─── Generic merge helpers (CLI > YAML > Props > ENV) ────────

    private fun mergeInt(
        cli: Map<String, Any?>,
        key: String,
        yaml: Int?,
        props: Int
    ): Int {
        cli.cliInt(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    private fun mergeDouble(
        cli: Map<String, Any?>,
        key: String,
        yaml: Double?,
        props: Double
    ): Double {
        cli.cliDouble(key)?.let { return it }
        yaml?.let { return it }
        return props
    }

    private fun mergeStrList(
        cli: Map<String, Any?>,
        key: String,
        yaml: List<String>?,
        props: List<String>,
        env: List<String>
    ): List<String> {
        val cliValue = cli[key]?.toString()
        if (!cliValue.isNullOrBlank()) return splitCommaList(cliValue)
        if (!yaml.isNullOrEmpty()) return yaml
        if (props.isNotEmpty()) return props
        return env
    }

    private fun Map<String, Any?>.cliInt(key: String): Int? =
        this[key]?.let { (it as? Int) ?: it.toString().toIntOrNull() }

    private fun Map<String, Any?>.cliDouble(key: String): Double? =
        this[key]?.let { (it as? Double) ?: it.toString().toDoubleOrNull() }

    /** Splits a comma-separated string into a trimmed list of non-blank entries. */
    internal fun splitCommaList(value: String): List<String> =
        value.split(",").map { it.trim() }.filter { it.isNotBlank() }
}
