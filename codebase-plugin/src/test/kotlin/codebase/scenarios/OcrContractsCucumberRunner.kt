package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@ocr-contracts` scenarios
 * (EPIC CDX-OCR-CONTRACTS US-4 — N0 OCR port boundary BDD).
 *
 * Targets `codebase_ocr_contracts.feature` and filters `@ocr-contracts`
 * tagged scenarios (pattern S-082 — `RagSocleCucumberRunner`,
 * `FineTuningCucumberRunner`). No `@integration` scenarios here — the
 * boundary is driven via a fake `VisionProvider` stub behind the real
 * `VisionOcrEngineAdapter` and the `OcrTask` AI-only rejection path,
 * no network, no API key, no real Gradle execution.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-ocr-contracts.html, json:build/reports/cucumber-ocr-contracts.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_ocr_contracts.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@ocr-contracts and not @integration")
class OcrContractsCucumberRunner
