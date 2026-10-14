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
 * CI shard — EPIC Y / W / X / K (agentic literature compiler schema/compiler/
 * ingestor/importer/enforcement, task auto-discovery, multi-step decision
 * plan→execute→verify→adapt, workspace ontology metadata contract, real Graphify
 * subgraph augmented context).
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/epic_y_3_agentic_schema.feature")
@SelectClasspathResource("features/epic_y_4_agentic_compiler.feature")
@SelectClasspathResource("features/epic_y_5_agentic_ingestor.feature")
@SelectClasspathResource("features/epic_y_6_external_importer.feature")
@SelectClasspathResource("features/epic_y_7_agentic_chunk_enforcement.feature")
@SelectClasspathResource("features/epic_w_4_list_tasks.feature")
@SelectClasspathResource("features/epic_x_1_vibecoding_plan.feature")
@SelectClasspathResource("features/epic_x_2_task_result_verifier.feature")
@SelectClasspathResource("features/epic_x_4_rollback_strategy.feature")
@SelectClasspathResource("features/epic_x_5_replan_catalog.feature")
@SelectClasspathResource("features/epic_x_6_e2e_plan_fail_adapt.feature")
@SelectClasspathResource("features/epic_k_metadata_contract.feature")
@SelectClasspathResource("features/codebase_subgraph.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/epic_y_3_agentic_schema.feature,src/test/features/epic_y_4_agentic_compiler.feature," +
        "src/test/features/epic_y_5_agentic_ingestor.feature,src/test/features/epic_y_6_external_importer.feature," +
        "src/test/features/epic_y_7_agentic_chunk_enforcement.feature,src/test/features/epic_w_4_list_tasks.feature," +
        "src/test/features/epic_x_1_vibecoding_plan.feature,src/test/features/epic_x_2_task_result_verifier.feature," +
        "src/test/features/epic_x_4_rollback_strategy.feature,src/test/features/epic_x_5_replan_catalog.feature," +
        "src/test/features/epic_x_6_e2e_plan_fail_adapt.feature,src/test/features/epic_k_metadata_contract.feature," +
        "src/test/features/codebase_subgraph.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-agentic.html, json:build/reports/cucumber-shard-agentic.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardAgenticRunner
