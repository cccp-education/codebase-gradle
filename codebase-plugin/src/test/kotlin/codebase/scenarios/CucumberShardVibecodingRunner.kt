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
 * CI shard — EPIC V / V-6 / V-8 / Z (vibecoding task + graph, Ollama pool and
 * resolver, session repository, dashboard, governance fallback, autofocus,
 * cross-borough).
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/epic_v_vibecoding.feature")
@SelectClasspathResource("features/epic_v6_ollama_cloud.feature")
@SelectClasspathResource("features/epic_v6_pool.feature")
@SelectClasspathResource("features/epic_v6_resolver.feature")
@SelectClasspathResource("features/epic_v_pool.feature")
@SelectClasspathResource("features/epic_v_session_repository.feature")
@SelectClasspathResource("features/epic_v_dashboard.feature")
@SelectClasspathResource("features/epic_v_local_governance_fallback.feature")
@SelectClasspathResource("features/epic_z_6_autofocus_feedback.feature")
@SelectClasspathResource("features/epic_z_7_cross_borough.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/epic_v_vibecoding.feature,src/test/features/epic_v6_ollama_cloud.feature," +
        "src/test/features/epic_v6_pool.feature,src/test/features/epic_v6_resolver.feature," +
        "src/test/features/epic_v_pool.feature,src/test/features/epic_v_session_repository.feature," +
        "src/test/features/epic_v_dashboard.feature,src/test/features/epic_v_local_governance_fallback.feature," +
        "src/test/features/epic_z_6_autofocus_feedback.feature,src/test/features/epic_z_7_cross_borough.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-vibecoding.html, json:build/reports/cucumber-shard-vibecoding.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardVibecodingRunner
