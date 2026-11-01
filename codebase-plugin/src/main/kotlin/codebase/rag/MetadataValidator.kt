package codebase.rag

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Validates a `metadata.json` pivot document (EPIC K).
 *
 * XB-FP-5 (arrow-kt adoption): the validation is expressed as
 * `Either<NonEmptyList<String>, Metadata>` — the independent checks
 * (version, type, dependencies) are **accumulated** instead of short-circuiting on
 * the first one, and the file/parse failures (which make further checks
 * meaningless) stay as an early `Left`. [validate] folds the result into the
 * historical sealed [ValidationResult], so the public API is unchanged.
 */
object MetadataValidator {
    private val log = LoggerFactory.getLogger(MetadataValidator::class.java)

    sealed class ValidationResult {
        data class Valid(val metadata: Metadata) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun validate(
        file: File,
        expectedVersion: String? = null,
        expectedType: String? = null,
    ): ValidationResult =
        validateEither(file, expectedVersion, expectedType).fold(
            ifLeft = { errors -> ValidationResult.Invalid(errors.joinToString("; ")) },
            ifRight = { metadata -> ValidationResult.Valid(metadata) },
        )

    /** Arrow-shaped validation: `Left` carries every violation, `Right` the metadata. */
    internal fun validateEither(
        file: File,
        expectedVersion: String? = null,
        expectedType: String? = null,
    ): Either<NonEmptyList<String>, Metadata> {
        if (!file.isFile) {
            return violation("metadata.json introuvable : ${file.absolutePath}")
        }

        val metadata: Metadata = when (val parsed = Either.catch { Metadata.fromFile(file) }) {
            is Either.Left -> return violation("metadata.json illisible : ${parsed.value.message}")
            is Either.Right -> parsed.value
        }

        val errors: List<String> =
            listOfNotNull(
                versionViolation(metadata, expectedVersion),
                typeViolation(metadata, expectedType),
            ) + metadata.dependencies.filter { it.isBlank() }.map { "Dépendance vide dans dependencies[]" }

        return when {
            errors.isEmpty() -> {
                log.info(
                    "MetadataValidator: {} validé — source={}, type={}, version={}, deps={}",
                    file.name,
                    metadata.source,
                    metadata.type,
                    metadata.version,
                    metadata.dependencies.size,
                )
                metadata.right()
            }
            else -> NonEmptyList(errors.first(), errors.drop(1)).left()
        }
    }

    private fun violation(message: String): Either<NonEmptyList<String>, Metadata> =
        NonEmptyList(message, emptyList()).left()

    private fun versionViolation(metadata: Metadata, expectedVersion: String?): String? {
        if (expectedVersion == null) return null
        val providedMajor = metadata.version.split(".").firstOrNull()?.toIntOrNull() ?: return null
        val expectedMajor = expectedVersion.split(".").firstOrNull()?.toIntOrNull() ?: return null
        return if (providedMajor != expectedMajor) {
            "Version incompatible : fourni=${metadata.version}, attendu majeure $expectedVersion"
        } else {
            null
        }
    }

    private fun typeViolation(metadata: Metadata, expectedType: String?): String? =
        if (expectedType != null && metadata.type != expectedType) {
            "Type inattendu : fourni=${metadata.type}, attendu=$expectedType"
        } else {
            null
        }

    fun isVersionCompatible(provided: String, expected: String): Boolean {
        val providedMajor = provided.split(".").firstOrNull()?.toIntOrNull() ?: return false
        val expectedMajor = expected.split(".").firstOrNull()?.toIntOrNull() ?: return false
        return providedMajor == expectedMajor
    }
}
