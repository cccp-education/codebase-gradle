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
    fun `valid gradle task passes validation`() {
        ExecGradleTool.validateGradleTask("compileKotlin")
        ExecGradleTool.validateGradleTask("test")
        ExecGradleTool.validateGradleTask("build")
        ExecGradleTool.validateGradleTask("publishToMavenLocal")
    }

    @Test
    fun `clean build is blacklisted`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("clean build")
        }
        assertTrue(exception.message!!.contains("blacklisted"))
    }

    @Test
    fun `refresh dependencies is blacklisted`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("build --refresh-dependencies")
        }
        assertTrue(exception.message!!.contains("blacklisted"))
    }

    @Test
    fun `rm task is blacklisted`() {
        val exception = assertFailsWith<SecurityException> {
            ExecGradleTool.validateGradleTask("rm -rf build")
        }
        assertTrue(exception.message!!.contains("blacklisted"))
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
