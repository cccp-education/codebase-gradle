package codebase.koog.agentic

import java.security.MessageDigest

data class ScannedFileEntry(
    val relativePath: String,
    val checksum: String
)

data class GovernanceFileSnapshot(
    val entries: List<ScannedFileEntry>
) {

    fun paths(): Set<String> = entries.map { it.relativePath }.toSet()

    fun checksumOf(path: String): String? = entries.firstOrNull { it.relativePath == path }?.checksum

    companion object {
        fun empty(): GovernanceFileSnapshot = GovernanceFileSnapshot(emptyList())

        fun fromScanned(files: List<ScannedFile>): GovernanceFileSnapshot =
            GovernanceFileSnapshot(files.map { ScannedFileEntry(it.relativePath, sha256(it.content)) })

        private fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}