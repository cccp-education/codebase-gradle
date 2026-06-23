package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-epic-v-9-11.html, json:build/reports/cucumber-epic-v-9-11.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/epic_v_9_11_agentic_compiler.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@epic_v_9_11")
class EpicV911CucumberRunner
