package codebase.koog.planning

enum class TaskVerdict {
    SUCCESS,
    FAILED,
    BLOCKED,
    UNKNOWN
}

data class TaskResult(
    val verdict: TaskVerdict,
    val errorMessage: String = ""
)

class TaskResultVerifier {

    fun verify(stdout: String, stderr: String): TaskResult =
        verify(stdout, stderr, DEFAULT_EXPECTED_OUTPUT)

    fun verify(stdout: String, stderr: String, expectedOutput: String): TaskResult {
        val combined = "$stdout\n$stderr".lowercase()
        val expected = expectedOutput.lowercase()

        if (combined.contains("dry run")) {
            return TaskResult(TaskVerdict.SUCCESS)
        }

        val isCustomExpected = expected != DEFAULT_EXPECTED_OUTPUT_LOWER

        if (isCustomExpected) {
            val hasBuildFailed = combined.contains("build failed") ||
                combined.contains("compilation error") ||
                hasActualFailure(combined)
            if (hasBuildFailed) {
                return TaskResult(TaskVerdict.FAILED, stderr.ifBlank { stdout })
            }
            return if (combined.contains(expected)) {
                TaskResult(TaskVerdict.SUCCESS)
            } else {
                TaskResult(TaskVerdict.FAILED, stderr.ifBlank { stdout })
            }
        }

        if (combined.contains("build successful") && !combined.contains("build failed")) {
            return TaskResult(TaskVerdict.SUCCESS)
        }

        if (combined.contains("build failed") ||
            combined.contains("compilation error") ||
            hasActualFailure(combined)
        ) {
            return TaskResult(TaskVerdict.FAILED, stderr.ifBlank { stdout })
        }

        if (combined.contains("not found") ||
            combined.contains("unknown project")
        ) {
            return TaskResult(TaskVerdict.BLOCKED, stderr.ifBlank { stdout })
        }

        return TaskResult(TaskVerdict.UNKNOWN, stderr.ifBlank { stdout })
    }

    private fun hasActualFailure(combined: String): Boolean {
        val failedMatches = FAILED_PATTERN.findAll(combined).toList()
        return failedMatches.any { match ->
            val before = combined.substring(0, match.range.first)
            !before.endsWith("0 ") && !before.endsWith("no ")
        }
    }

    companion object {
        const val DEFAULT_EXPECTED_OUTPUT = "BUILD SUCCESSFUL"
        const val DEFAULT_EXPECTED_OUTPUT_LOWER = "build successful"
        private val FAILED_PATTERN = Regex("\\bfailed\\b")
    }
}
