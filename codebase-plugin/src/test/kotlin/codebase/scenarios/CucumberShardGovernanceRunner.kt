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
 * CI shard — EPIC V-9 (agentic governance compilation: scan agent, chunk
 * validator, governance ontologizer/ingest, validation gate/report, agentic
 * compiler/executor, tool registry enforcement, quarantine, strict mode).
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/epic_v_9_3_scan_agent.feature")
@SelectClasspathResource("features/epic_v_9_5_chunk_validator.feature")
@SelectClasspathResource("features/epic_v_9_7_governance_ontologizer.feature")
@SelectClasspathResource("features/epic_v_9_8_governance_ingest.feature")
@SelectClasspathResource("features/epic_v_9_9_governance_validation_gate.feature")
@SelectClasspathResource("features/epic_v_9_10_chunk_validation_report.feature")
@SelectClasspathResource("features/epic_v_9_11_agentic_compiler.feature")
@SelectClasspathResource("features/epic_v_9_12_agentic_executor.feature")
@SelectClasspathResource("features/epic_v_9_13_tool_registry_enforcement.feature")
@SelectClasspathResource("features/epic_v_9_14_auto_activation_hook.feature")
@SelectClasspathResource("features/epic_v_9_15_invalid_chunk_quarantine.feature")
@SelectClasspathResource("features/epic_v_9_16_strict_validation_mode.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/epic_v_9_3_scan_agent.feature,src/test/features/epic_v_9_5_chunk_validator.feature," +
        "src/test/features/epic_v_9_7_governance_ontologizer.feature,src/test/features/epic_v_9_8_governance_ingest.feature," +
        "src/test/features/epic_v_9_9_governance_validation_gate.feature,src/test/features/epic_v_9_10_chunk_validation_report.feature," +
        "src/test/features/epic_v_9_11_agentic_compiler.feature,src/test/features/epic_v_9_12_agentic_executor.feature," +
        "src/test/features/epic_v_9_13_tool_registry_enforcement.feature,src/test/features/epic_v_9_14_auto_activation_hook.feature," +
        "src/test/features/epic_v_9_15_invalid_chunk_quarantine.feature,src/test/features/epic_v_9_16_strict_validation_mode.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-governance.html, json:build/reports/cucumber-shard-governance.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardGovernanceRunner
