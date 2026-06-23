package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite

/**
 * Runner Cucumber dédié aux scénarios @epic_v6_pool.
 *
 * V-6-POOL-3 : Test vibecoding complet avec gemma4:31b-cloud via FakeProvider
 * (zéro réseau, déterministe).
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-epic-v6-pool.html, json:build/reports/cucumber-epic-v6-pool.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/epic_v6_pool.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@epic_v6_pool")
class EpicV6PoolCucumberRunner