package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to @subgraph scenarios
 * (EPIC SUBGRAPH — real Graphify subgraph in augmented context).
 *
 * Targets codebase_subgraph.feature and filters @subgraph tagged scenarios.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-subgraph.html, json:build/reports/cucumber-subgraph.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_subgraph.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@subgraph")
class SubgraphCucumberRunner