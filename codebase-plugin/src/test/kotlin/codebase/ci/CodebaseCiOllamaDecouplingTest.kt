package codebase.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * CI Ollama decoupling guard.
 *
 * The JUnit `test` task is the bulk of the `unit` CI job, and only a handful of
 * tests probe a real Ollama instance (the Docker image + model setup costs ~2.5
 * min). Those tests are marked `@Tag("ollama")`, excluded from `test`, and run by
 * the dedicated `unitOllama` task / CI `unit-ollama` job.
 *
 * This guard kills the two failure modes of that split, from the repository
 * sources only (no Gradle model, no test execution):
 * 1. **Decoupling drift** — the `unit` job must not re-introduce the Ollama
 *    Docker setup, and a `unit-ollama` job must exist and invoke `unitOllama`.
 * 2. **Tag drift** — every test source that reaches a real Ollama instance
 *    (`OLLAMA_TEST_PORT`) must carry the `@Tag("ollama")` marker, so a new
 *    Ollama-dependent test can never be silently excluded from both jobs.
 */
class CodebaseCiOllamaDecouplingTest {

    private val repoRoot: File = run {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !dir.resolve(".github/workflows/test.yml").isFile) {
            dir = dir.parentFile
        }
        check(dir.resolve(".github/workflows/test.yml").isFile) {
            "test.yml not found above ${System.getProperty("user.dir")}"
        }
        dir
    }

    private val testSourcesDir = repoRoot.resolve("codebase-plugin/src/test/kotlin")
    private val buildScript = repoRoot.resolve("codebase-plugin/build.gradle.kts")
    private val workflow = repoRoot.resolve(".github/workflows/test.yml")

    /** Extracts the YAML block of a top-level job (2-space indented `name:`). */
    private fun jobBlock(name: String): String {
        val text = workflow.readText()
        val marker = "\n  $name:"
        val start = text.indexOf(marker)
        check(start >= 0) { "job '$name' not found in test.yml" }
        val from = start + marker.length
        val next = Regex("""(?m)^  [a-zA-Z0-9][a-zA-Z0-9-]*:""")
            .findAll(text)
            .map { it.range.first }
            .firstOrNull { it > from }
        return if (next != null) text.substring(from, next) else text.substring(from)
    }

    @Test
    fun `the plain unit job no longer sets up Ollama`() {
        val unit = jobBlock("unit")
        assertThat(unit)
            .describedAs("the unit job must run the plain test task")
            .contains("./gradlew test")
        assertThat(unit)
            .describedAs("the unit job must NOT pull the Ollama Docker image")
            .doesNotContain("ollama/ollama")
        assertThat(unit)
            .describedAs("the unit job must NOT start Ollama instances")
            .doesNotContain("OLLAMA_TEST_PORT")
    }

    @Test
    fun `a dedicated unit-ollama job runs unitOllama with the Docker setup`() {
        val ollama = jobBlock("unit-ollama")
        assertThat(ollama)
            .describedAs("the unit-ollama job must run the unitOllama task")
            .contains("./gradlew unitOllama")
        assertThat(ollama)
            .describedAs("the unit-ollama job must provide the Ollama Docker image")
            .contains("ollama/ollama")
        assertThat(ollama)
            .describedAs("the unit-ollama job must inject the CI Ollama env vars")
            .contains("OLLAMA_TEST_PORT")
    }

    @Test
    fun `the build script excludes the ollama tag from test and registers unitOllama`() {
        val build = buildScript.readText()
        assertThat(build)
            .describedAs("build.gradle.kts must register the dedicated unitOllama task")
            .contains("unitOllama")
        assertThat(build)
            .describedAs("the test task must exclude the ollama tag")
            .contains("excludeTags(\"ollama\")")
    }

    @Test
    fun `every test source reaching a real Ollama instance carries the ollama tag`() {
        val offenders = testSourcesDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            // The guards live in codebase/ci and mention OLLAMA_TEST_PORT in
            // assertions — they are not test sources that reach Ollama.
            .filterNot { it.parentFile.name == "ci" }
            .filter { it.readText().contains("OLLAMA_TEST_PORT") }
            .filterNot { it.readText().contains("@Tag(\"ollama\")") }
            .map { it.relativeTo(repoRoot).path }
            .toList()

        assertThat(offenders)
            .describedAs("test sources reading OLLAMA_TEST_PORT must be @Tag(\"ollama\")")
            .isEmpty()
    }
}
