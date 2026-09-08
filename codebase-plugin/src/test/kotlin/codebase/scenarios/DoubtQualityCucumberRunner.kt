package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@doubt-quality` scenarios
 * (EPIC OCR-QUALITY US-4 — doubt metadata ingestion + exposure BDD).
 *
 * Targets `codebase_doubt_quality.feature` and filters `@doubt-quality`
 * tagged scenarios (pattern S-082 — `RagSocleCucumberRunner`,
 * `OcrContractsCucumberRunner`). No `@integration` scenarios here — the
 * domain is driven via fakes/stubs (`DoubtStubRagStore`), no network,
 * no real pgvector, no Gradle task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-doubt-quality.html, json:build/reports/cucumber-doubt-quality.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_doubt_quality.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@doubt-quality and not @integration")
class DoubtQualityCucumberRunner