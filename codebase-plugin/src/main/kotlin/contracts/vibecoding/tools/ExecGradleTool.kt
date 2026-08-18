package contracts.vibecoding.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.serialization.typeToken
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.TimeUnit

private val GRADLE_TASK_ALLOWLIST = listOf(
    Regex("^(build|compileKotlin|compileTestKotlin|test|testFast|check|assemble|jar|publishToMavenLocal|tasks)$"),
    Regex("^:?[\\w-]+:[\\w-]+$")
)

private val GRADLE_ARG_ALLOWLIST = listOf(
    Regex("^-P[\\w.]+=\\S+$"),
    Regex("^--\\w+(?:=\\S+)?$")
)

private fun isBareLongFlag(token: String): Boolean =
    token.startsWith("--") && !token.contains('=') && GRADLE_ARG_ALLOWLIST.any { it.matches(token) }

private val GRADLE_DENYLIST = listOf(
    Regex("clean\\s+build", RegexOption.IGNORE_CASE),
    Regex("--refresh-dependencies", RegexOption.IGNORE_CASE),
    Regex("\\brm\\s", RegexOption.IGNORE_CASE),
    Regex("\\bdelete\\s", RegexOption.IGNORE_CASE),
    Regex("\\bsudo\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcurl\\b", RegexOption.IGNORE_CASE),
    Regex(";"),
    Regex("\\|")
)

object ExecGradleTool : SimpleTool<ExecGradleTool.Args>(
    argsType = typeToken<Args>(),
    name = "__exec_gradle__",
        description = "Execute a Gradle task via ./gradlew. " +
        "Returns EXIT code + stdout (max 8000 chars). " +
        "Use for build, test, compile, publish, etc. " +
        "Allowlist deny-by-default: only listed tasks (build, compileKotlin, test, check, jar, publishToMavenLocal, tasks, project:task form) pass; " +
        "args limited to -Pproperty=value, --long-flag, --long-flag=value, and --long-flag value (paired). " +
        "Deny-list second ride rejects clean build, --refresh-dependencies, rm, delete, sudo, curl, semicolon, pipe."
) {
    @Serializable
    data class Args(
        val task: String,
        val workingDir: String = "."
    )

    override suspend fun execute(args: Args): String {
        validateGradleTask(args.task)

        val workingDir = args.workingDir
        val process = ProcessBuilder("./gradlew", args.task)
            .directory(File(workingDir))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().take(8000)
        val exited = process.waitFor(120, TimeUnit.SECONDS)
        if (!exited) {
            process.destroyForcibly()
            return "GRADLE EXIT: 124 (timeout)\n$output"
        }
        return "GRADLE EXIT: ${process.exitValue()}\n$output"
    }

    fun executeBlocking(task: String, workingDir: String = "."): String {
        validateGradleTask(task)
        return runBlocking { execute(Args(task, workingDir)) }
    }

    fun validateGradleTask(task: String) {
        val tokens = task.trim().split(Regex("\\s+"))
        val allowed = tokens.withIndex().all { (index, token) ->
            when (index) {
                0 -> GRADLE_TASK_ALLOWLIST.any { it.matches(token) }
                else -> {
                    val previous = tokens[index - 1]
                    if (GRADLE_ARG_ALLOWLIST.any { it.matches(token) }) {
                        true
                    } else if (isBareLongFlag(previous)) {
                        !token.startsWith("-") && !token.matches(Regex(".*[;&|].*"))
                    } else {
                        false
                    }
                }
            }
        }
        if (!allowed) {
            throw SecurityException(
                "Gradle task denied: not in allowlist (deny-by-default)"
            )
        }
        for (pattern in GRADLE_DENYLIST) {
            if (pattern.containsMatchIn(task)) {
                throw SecurityException(
                    "Gradle task denied: matches denied pattern '${pattern.pattern}'"
                )
            }
        }
    }
}