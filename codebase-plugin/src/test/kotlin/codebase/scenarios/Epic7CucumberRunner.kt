package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-epic-7.html, json:build/reports/cucumber-epic-7.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/epic_7_expert_dispatcher.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@epic7")
class Epic7CucumberRunner
