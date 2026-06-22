package codebase.koog.agentic

import java.io.File

class ScanAgent {

    private val ignoredDirs = setOf("build", ".git", ".gradle", "node_modules")

    fun scan(root: File): List<ScannedFile> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val results = mutableListOf<ScannedFile>()
        scanDir(root, root, results)
        return results
    }

    private fun scanDir(dir: File, root: File, acc: MutableList<ScannedFile>) {
        dir.listFiles()?.forEach { entry ->
            when {
                entry.isDirectory && entry.name in ignoredDirs -> return@forEach
                entry.isDirectory -> scanDir(entry, root, acc)
                entry.isFile && entry.extension == "adoc" -> {
                    val rel = root.toPath().relativize(entry.toPath()).toString()
                        .replace(File.separatorChar, '/')
                    acc.add(ScannedFile(relativePath = rel, content = entry.readText(Charsets.UTF_8)))
                }
            }
        }
    }
}