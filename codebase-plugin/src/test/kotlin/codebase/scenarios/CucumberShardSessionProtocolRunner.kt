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
 * CI shard — EPIC SP (opencode↔Gradle session protocol: protocol, server daemon,
 * lifecycle, E2E, event stream, live context), plus the autonomous session
 * orchestration (SVO), the LlmBuildService bridge (SLD-8) and the cross-borough
 * translation service.
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/epic_sp_1_session_protocol.feature")
@SelectClasspathResource("features/epic_sp_2_session_server.feature")
@SelectClasspathResource("features/epic_sp_3_lifecycle.feature")
@SelectClasspathResource("features/epic_sp_4_e2e.feature")
@SelectClasspathResource("features/epic_sp_5_event_stream.feature")
@SelectClasspathResource("features/epic_sp_6_live_context.feature")
@SelectClasspathResource("features/autonomous_session.feature")
@SelectClasspathResource("features/epic_sld8_llm_service.feature")
@SelectClasspathResource("features/epic_translation.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/epic_sp_1_session_protocol.feature,src/test/features/epic_sp_2_session_server.feature," +
        "src/test/features/epic_sp_3_lifecycle.feature,src/test/features/epic_sp_4_e2e.feature," +
        "src/test/features/epic_sp_5_event_stream.feature,src/test/features/epic_sp_6_live_context.feature," +
        "src/test/features/autonomous_session.feature,src/test/features/epic_sld8_llm_service.feature," +
        "src/test/features/epic_translation.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-session-protocol.html, json:build/reports/cucumber-shard-session-protocol.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardSessionProtocolRunner
