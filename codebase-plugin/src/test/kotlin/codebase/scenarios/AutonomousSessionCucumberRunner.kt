package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@autonomous-session` scenarios
 * (EPIC SVO-4/5 — autonomous session orchestration BDD).
 *
 * Targets `autonomous_session.feature` and filters `@autonomous-session`
 * scenarios (pattern S-082 — `CorpusIngestCucumberRunner`). No
 * `@integration` scenarios here — the session loop is a fake (no
 * Gradle task, no LLM, no network).
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-autonomous-session.html, json:build/reports/cucumber-autonomous-session.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/autonomous_session.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@autonomous-session and not @integration")
class AutonomousSessionCucumberRunner
