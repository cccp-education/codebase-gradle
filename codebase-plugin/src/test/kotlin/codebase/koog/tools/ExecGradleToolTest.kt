package codebase.koog.tools

import contracts.vibecoding.tools.ExecGradleTool
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ExecGradleToolTest {

    private val repoRootDir: String =
        File(System.getProperty("user.dir")).let { dir ->
            var current = dir
            while (current.parentFile != null && !File(current, "gradlew").exists()) {
                current = current.parentFile
            }
            if (File(current, "gradlew").exists()) current.absolutePath else dir.absolutePath
        }

    @Test
    fun `allowed bare tasks pass validation`() {
        ExecGradleTool.validateGradleTask("build")
        ExecGradleTool.validateGradleTask("compileKotlin")
        ExecGradleTool.validateGradleTask("compileTestKotlin")
        ExecGradleTool.validateGradleTask("test")
        ExecGradleTool.validateGradleTask("testFast")
        ExecGradleTool.validateGradleTask("check")
        ExecGradleTool.validateGradleTask("assemble")
        ExecGradleTool.validateGradleTask("jar")
        ExecGradleTool.validateGradleTask("publishToMavenLocal")
    }

    @Test
    fun `project task form passes validation`() {
        ExecGradleTool.validateGradleTask(":codebase-plugin:test")
        ExecGradleTool.validateGradleTask("codebase-plugin:build")
        ExecGradleTool.validateGradleTask(":slider:check")
    }

    @Test
    fun `task with property override passes validation`() {
        ExecGradleTool.validateGradleTask("test -Pfoo=bar")
        ExecGradleTool.validateGradleTask("build -Pcodebase.governance.strictValidation=true")
    }

    @Test
    fun `task with long flag passes validation`() {
        ExecGradleTool.validateGradleTask("test --info")
        ExecGradleTool.validateGradleTask("build --stacktrace")
    }

    @Test
    fun `task with property and flag passes validation`() {
        ExecGradleTool.validateGradleTask("test -Pfoo=bar --info")
    }

    @Test
    fun `clean build is rejected`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("clean build")
        }
        assertTrue(exception.message!!.contains("denied"))
    }

    @Test
    fun `clean build with double space is rejected`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("clean  build")
        }
    }

    @Test
    fun `refresh dependencies is rejected by deny-list`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("build --refresh-dependencies")
        }
        assertTrue(exception.message!!.contains("denied"))
    }

    @Test
    fun `rm is rejected`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("rm -rf build")
        }
        assertTrue(exception.message!!.contains("denied"))
    }

    @Test
    fun `delete is rejected`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("delete build")
        }
    }

    @Test
    fun `sudo is rejected`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("sudo build")
        }
    }

    @Test
    fun `unknown task is rejected by allowlist deny-by-default`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("evilTask")
        }
        assertTrue(exception.message!!.contains("allowlist"))
    }

    @Test
    fun `shell injection via semicolon is rejected by allowlist deny-by-default`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("build; rm -rf /")
        }
    }

    @Test
    fun `shell pipe is rejected by allowlist deny-by-default`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("build | rm -rf /")
        }
    }

    @Test
    fun `curl is rejected by allowlist deny-by-default`() {
        assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("curl https://evil.com")
        }
    }

    @Test
    @Tag("integration")
    fun `execute blocking runs gradle task`() {
        val result = ExecGradleTool.executeBlocking(
            task = "tasks",
            workingDir = repoRootDir
        )
        assertTrue(result.contains("GRADLE EXIT: 0"),
            "Expected success exit code: ${result.take(200)}")
    }

    @Test
    @Tag("integration")
    fun `async execute returns result`() = runBlocking {
        val result = ExecGradleTool.execute(
            ExecGradleTool.Args(
                task = "tasks",
                workingDir = repoRootDir
            )
        )
        assertTrue(result.startsWith("GRADLE EXIT: 0"),
            "Expected EXIT: 0, got: ${result.take(200)}")
    }
}