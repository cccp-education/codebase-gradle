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
 * Cucumber runner dedicated to `@koog-pipeline` scenarios (EPIC
 * KOOG-PIPELINE-DEDUP, BS-1 — generic N1 koog pipeline skeleton).
 *
 * Targets `koog_pipeline_dedup.feature` and filters `@koog-pipeline` tagged
 * scenarios (pattern S-082 — `RagSocleCucumberRunner`,
 * `PageRetrievalCucumberRunner`). No `@integration` scenarios: the skeleton is
 * driven through the in-memory `KoogPipelineDefinition`, no network, no
 * Gradle task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-koog-pipeline.html, json:build/reports/cucumber-koog-pipeline.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/koog_pipeline_dedup.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@koog-pipeline and not @integration")
class KoogPipelineCucumberRunner
