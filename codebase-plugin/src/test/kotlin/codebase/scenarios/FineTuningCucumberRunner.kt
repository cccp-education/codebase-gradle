package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@finetuning` scenarios
 * (EPIC FT-PIPELINE US-5 — fine-tuning N1 pipeline BDD).
 *
 * Targets `codebase_finetuning.feature` and filters `@finetuning` tagged
 * scenarios (pattern S-082 — `SubgraphCucumberRunner`,
 * `VibeHardening2CucumberRunner`). No `@integration` scenarios here —
 * the domain is driven via fakes/stubs, no network, no Gradle task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-finetuning.html, json:build/reports/cucumber-finetuning.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_finetuning.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@finetuning and not @integration")
class FineTuningCucumberRunner