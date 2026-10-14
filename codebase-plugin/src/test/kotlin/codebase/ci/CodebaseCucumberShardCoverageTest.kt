package codebase.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * CI shard coverage guard (CI sharding).
 *
 * The monolithic `testAll` BDD run is split in `.github/workflows/test.yml` into
 * exhaustive, disjoint feature shards, each run on its own CI machine. This guard
 * kills the "test fantôme" risk (S-269): a feature that belongs to no shard (or to
 * two) would silently stop being executed.
 *
 * It asserts, from the repository sources (no Gradle model, pure file parsing):
 * 1. the shard runners form a **disjoint** partition whose union is **every**
 *    feature file under `src/test/features/`, and each shard preserves the
 *    aggregate exclusion (`not @integration`);
 * 2. every shard runner is registered as a Gradle task **and** invoked by the CI
 *    workflow (`.github/workflows/test.yml`);
 * 3. the `unit` job runs the plain `test` task.
 */
class CodebaseCucumberShardCoverageTest {

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

    private val scenarioDir = repoRoot.resolve("codebase-plugin/src/test/kotlin/codebase/scenarios")
    private val featuresDir = repoRoot.resolve("codebase-plugin/src/test/features")
    private val buildScript = repoRoot.resolve("codebase-plugin/build.gradle.kts")
    private val workflow = repoRoot.resolve(".github/workflows/test.yml")

    private fun allFeatures(): Set<String> =
        featuresDir.listFiles { f -> f.isFile && f.name.endsWith(".feature") }!!
            .map { it.name }
            .toSet()

    private fun shardRunners(): List<File> =
        scenarioDir.listFiles { f -> f.isFile && f.name.startsWith("CucumberShard") && f.name.endsWith("Runner.kt") }!!
            .sortedBy { it.name }

    private fun runnerFeatures(runner: File): List<String> =
        Regex("""@SelectClasspathResource\("features/([^"]+\.feature)"\)""")
            .findAll(runner.readText())
            .map { it.groupValues[1] }
            .toList()

    private fun runnerTags(runner: File): String =
        Regex("""FILTER_TAGS_PROPERTY_NAME,\s*value = "([^"]*)"""")
            .find(runner.readText())?.groupValues?.get(1) ?: ""

    @Test
    fun `the shards cover every feature exactly once and exclude integration`() {
        val runners = shardRunners()
        assertThat(runners)
            .describedAs("CI shard runners must exist and be at least 5")
            .hasSizeGreaterThanOrEqualTo(5)

        runners.forEach { r ->
            assertThat(runnerFeatures(r))
                .describedAs("%s must select at least one feature", r.name)
                .isNotEmpty()
            assertThat(runnerTags(r))
                .describedAs("%s must exclude @integration (aggregate contract)", r.name)
                .contains("not @integration")
        }

        val byFeature = mutableMapOf<String, MutableList<File>>()
        runners.forEach { r -> runnerFeatures(r).forEach { byFeature.getOrPut(it) { mutableListOf() }.add(r) } }

        assertThat(byFeature.keys)
            .describedAs("the shards must cover every feature file")
            .containsExactlyInAnyOrderElementsOf(allFeatures())

        assertThat(byFeature.filterValues { it.size > 1 }.keys)
            .describedAs("a feature must belong to exactly one shard (disjoint partition)")
            .isEmpty()
    }

    @Test
    fun `every shard runner is a Gradle task and is run by the CI workflow`() {
        val runnerClasses = shardRunners().map { it.nameWithoutExtension }
        val build = buildScript.readText()
        val ci = workflow.readText()

        runnerClasses.forEach { runner ->
            assertThat(build)
                .describedAs("%s must be registered as a CucumberTaskSpec", runner)
                .contains(runner)
        }

        val buildShardTasks = Regex("""cucumberTestShard[A-Za-z0-9]+""")
            .findAll(build).map { it.value }.toSet()
        val ciShardTasks = Regex("""cucumberTestShard[A-Za-z0-9]+""")
            .findAll(ci).map { it.value }.toSet()

        assertThat(buildShardTasks)
            .describedAs("shard CucumberTaskSpec tasks")
            .hasSizeGreaterThanOrEqualTo(5)
        assertThat(ciShardTasks)
            .describedAs("test.yml must run every shard task")
            .containsExactlyInAnyOrderElementsOf(buildShardTasks)
    }

    @Test
    fun `the unit job runs the plain test task`() {
        assertThat(workflow.readText())
            .describedAs("test.yml must run the unit `test` task")
            .contains("./gradlew test")
    }
}
