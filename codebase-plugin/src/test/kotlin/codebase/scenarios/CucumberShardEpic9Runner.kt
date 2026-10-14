package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CI shard — EPIC 9 (opencode augmented context: pgvector container, dataset
 * embeddings, workspace walker, indexation, batch validation, vector query,
 * composite context, opencode injector, benchmark, vision/opinion).
 *
 * One of the exhaustive, disjoint feature shards that replace the single
 * monolithic `testAll` invocation in CI (`.github/workflows/test.yml`), so the
 * BDD suite runs on several machines in parallel instead of one job. Each shard
 * keeps the one-JVM efficiency of the aggregate runner (no per-feature bootstrap).
 *
 * Exhaustiveness/disjointness is guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/9_1_pgvector_container.feature")
@SelectClasspathResource("features/9_2_3_dataset_embeddings.feature")
@SelectClasspathResource("features/9_3_4_workspace_walker_adoc.feature")
@SelectClasspathResource("features/9_5_indexation_metadata.feature")
@SelectClasspathResource("features/9_6_indexation_yaml_json.feature")
@SelectClasspathResource("features/9_7_batch_validation.feature")
@SelectClasspathResource("features/9_8_vector_query.feature")
@SelectClasspathResource("features/9_9_filtered_query.feature")
@SelectClasspathResource("features/9_10_composite_context.feature")
@SelectClasspathResource("features/9_11_opencode_injector.feature")
@SelectClasspathResource("features/9_12_augment_opencode.feature")
@SelectClasspathResource("features/9_13_pertinence_benchmark.feature")
@SelectClasspathResource("features/9_14_vision_opinion.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/9_1_pgvector_container.feature,src/test/features/9_2_3_dataset_embeddings.feature," +
        "src/test/features/9_3_4_workspace_walker_adoc.feature,src/test/features/9_5_indexation_metadata.feature," +
        "src/test/features/9_6_indexation_yaml_json.feature,src/test/features/9_7_batch_validation.feature," +
        "src/test/features/9_8_vector_query.feature,src/test/features/9_9_filtered_query.feature," +
        "src/test/features/9_10_composite_context.feature,src/test/features/9_11_opencode_injector.feature," +
        "src/test/features/9_12_augment_opencode.feature,src/test/features/9_13_pertinence_benchmark.feature," +
        "src/test/features/9_14_vision_opinion.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-epic9.html, json:build/reports/cucumber-shard-epic9.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardEpic9Runner
