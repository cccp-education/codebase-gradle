package codebase.koog.expert

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.work.DisableCachingByDefault
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.time.Instant

@Serializable
private data class ManifestJson(
    val version: String,
    val generatedAt: String,
    val experts: List<ExpertJson>
)

@Serializable
private data class ExpertJson(
    val domain: String,
    val label: String,
    val modelName: String,
    val baseUrl: String,
    val timeoutSeconds: Long
)

@DisableCachingByDefault(because = "Expert exposure — génération non-déterministe, dépend du registre runtime")
abstract class ExpertExposureTask : DefaultTask() {

    @get:Internal
    var expertRegistry: ExpertRegistry = ExpertRegistry()

    @get:Input
    @get:Optional
    abstract val domains: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val anonymizeEndpoints: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "generate"
        description = "Expose les experts enregistrés via Ollama — génère un manifest JSON/YAML consommable par slider/plantuml/bakery"
        domains.convention(emptyList())
        anonymizeEndpoints.convention(true)
    }

    @TaskAction
    fun executeExposure() {
        val allRegistrations = expertRegistry.listDomains().mapNotNull { domain ->
            expertRegistry.resolve(domain)
        }

        val filtered = if (domains.get().isEmpty()) {
            allRegistrations
        } else {
            val domainSet = domains.get().toSet()
            allRegistrations.filter { it.domain.name in domainSet }
        }

        val entries = filtered.map { ExpertExposureEntry.from(it) }
        val finalEntries = if (anonymizeEndpoints.get()) {
            entries.map { it.anonymize() }
        } else {
            entries
        }

        val manifest = ExpertExposureManifest(
            version = "1.0",
            generatedAt = Instant.now().toString(),
            experts = finalEntries
        )

        val json = Json { prettyPrint = true }
        val manifestJson = ManifestJson(
            version = manifest.version,
            generatedAt = manifest.generatedAt,
            experts = finalEntries.map { entry ->
                ExpertJson(
                    domain = entry.domain.name,
                    label = entry.domain.label,
                    modelName = entry.modelName,
                    baseUrl = entry.baseUrl,
                    timeoutSeconds = entry.timeoutSeconds
                )
            }
        )

        val output = json.encodeToString(manifestJson)
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(output)
        }
    }
}
