package codebase.koog.expert

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class ExpertManifestException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

@Serializable
private data class ReaderManifestJson(
    val version: String,
    val generatedAt: String,
    val experts: List<ReaderExpertJson>
)

@Serializable
private data class ReaderExpertJson(
    val domain: String,
    val label: String,
    val modelName: String,
    val baseUrl: String,
    val timeoutSeconds: Long
)

object ExpertManifestReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun read(file: File): ExpertExposureManifest {
        val raw = try {
            file.readText()
        } catch (e: IOException) {
            throw ExpertManifestException("Cannot read manifest file: ${file.absolutePath}", e)
        }
        return parse(raw)
    }

    fun readOrEmpty(file: File): ExpertExposureManifest =
        try {
            read(file)
        } catch (e: ExpertManifestException) {
            ExpertExposureManifest(version = "0.0", generatedAt = "", experts = emptyList())
        }

    private fun parse(raw: String): ExpertExposureManifest {
        val parsed = try {
            json.decodeFromString<ReaderManifestJson>(raw)
        } catch (e: SerializationException) {
            throw ExpertManifestException("Malformed manifest JSON", e)
        } catch (e: IllegalArgumentException) {
            throw ExpertManifestException("Malformed manifest JSON", e)
        }
        val entries = parsed.experts.map { ej ->
            ExpertExposureEntry(
                domain = ExpertDomain(ej.domain, ej.label),
                modelName = ej.modelName,
                baseUrl = ej.baseUrl,
                timeoutSeconds = ej.timeoutSeconds
            )
        }
        return ExpertExposureManifest(
            version = parsed.version,
            generatedAt = parsed.generatedAt,
            experts = entries
        )
    }
}

fun ExpertExposureManifest.findByDomain(name: String): ExpertExposureEntry? =
    experts.firstOrNull { it.domain.name == name }