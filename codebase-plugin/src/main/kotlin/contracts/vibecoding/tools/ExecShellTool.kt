package contracts.vibecoding.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.serialization.typeToken
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.TimeUnit

private val SHELL_ALLOWLIST = listOf(
    Regex("git\\s+(status|diff|log|show|branch|ls-files|check-ignore)\\b"),
    Regex("rg\\s+\\S"),
    Regex("ls(\\s+-[a-zA-Z]+)?\\s+\\S"),
    Regex("find\\s+\\.\\s"),
    Regex("mkdir\\s+/tmp/\\S"),
    Regex("echo\\s+\\S"),
    Regex("cat\\s+\\S"),
    Regex("(head|tail)\\s+\\S"),
    Regex("\\./gradlew\\s+\\S"),
    Regex("\\bpwd\\b")
)

private val SHELL_DENYLIST = listOf(
    Regex("\\brm\\s+-[a-zA-Z]*r", RegexOption.IGNORE_CASE),
    Regex("\\bsudo\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcurl\\b", RegexOption.IGNORE_CASE),
    Regex("\\bwget\\b", RegexOption.IGNORE_CASE),
    Regex("chmod\\s+0?777\\b", RegexOption.IGNORE_CASE),
    Regex("\\|\\s*sh\\b"),
    Regex("\\|\\s*bash\\b"),
    Regex("/etc/"),
    Regex("/dev/"),
    Regex(">\\s*/")
)

object ExecShellTool : SimpleTool<ExecShellTool.Args>(
    argsType = typeToken<Args>(),
    name = "__exec_shell__",
    description = "Execute a shell command via bash -c. " +
        "Returns EXIT code + stdout (max 8000 chars). " +
        "Working directory defaults to workspaceRoot. " +
        "Allowlist deny-by-default: only listed commands (git read, rg, ls, find, mkdir /tmp, echo, cat, head, tail, ./gradlew, pwd) pass; " +
        "deny-list second ride rejects rm, sudo, curl, wget, chmod 777, pipe to sh/bash, /etc/, /dev/, redirect to /."
) {
    @Serializable
    data class Args(
        val command: String,
        val workingDir: String = "."
    )

    override suspend fun execute(args: Args): String {
        validateCommand(args.command)

        val workingDir = args.workingDir
        val process = ProcessBuilder("bash", "-c", args.command)
            .directory(File(workingDir))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().take(8000)
        val exited = process.waitFor(120, TimeUnit.SECONDS)
        if (!exited) {
            process.destroyForcibly()
            return "EXIT: 124 (timeout)\n$output"
        }
        return "EXIT: ${process.exitValue()}\n$output"
    }

    fun executeBlocking(command: String, workingDir: String = "."): String {
        validateCommand(command)
        return runBlocking { execute(Args(command, workingDir)) }
    }

    fun executeBlocking(command: String, workingDir: String, timeoutMs: Int): String {
        validateCommand(command)
        val workingDirFile = File(workingDir)
        val process = ProcessBuilder("bash", "-c", command)
            .directory(workingDirFile)
            .redirectErrorStream(true)
            .start()

        val exited = process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        return if (!exited) {
            process.destroyForcibly()
            throw SecurityException("Shell command rejected: timeout after ${timeoutMs}ms")
        } else {
            val output = process.inputStream.bufferedReader().readText().take(8000)
            "EXIT: ${process.exitValue()}\n$output"
        }
    }

    fun validateCommand(command: String) {
        val allowed = SHELL_ALLOWLIST.any { it.containsMatchIn(command) }
        if (!allowed) {
            throw SecurityException(
                "Shell command denied: not in allowlist (deny-by-default)"
            )
        }
        for (pattern in SHELL_DENYLIST) {
            if (pattern.containsMatchIn(command)) {
                throw SecurityException(
                    "Shell command denied: matches denied pattern '${pattern.pattern}'"
                )
            }
        }
    }
}