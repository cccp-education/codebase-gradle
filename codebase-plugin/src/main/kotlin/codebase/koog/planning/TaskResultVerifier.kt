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

    fun verify(stdout: String, stderr: String): TaskResult {
        val combined = "$stdout\n$stderr".lowercase()

        if (combined.contains("build successful") && !combined.contains("build failed")) {
            return TaskResult(TaskVerdict.SUCCESS)
        }

        if (combined.contains("build failed") ||
            combined.contains("tests failed") ||
            combined.contains("compilation error") ||
            combined.contains("failed")
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
}
