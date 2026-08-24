package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@rag-socle` scenarios
 * (EPIC CDX-RAG-SOCLE US-5 — RagVectorStore socle BDD).
 *
 * Targets `codebase_rag_socle.feature` and filters `@rag-socle` tagged
 * scenarios (pattern S-082 — `FineTuningCucumberRunner`,
 * `SubgraphCucumberRunner`). No `@integration` scenarios here — the
 * domain is driven via fakes/stubs (`StubRagStore`) and the real
 * `RagVectorStore` (instantiated, never queried), no network, no Gradle
 * task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-rag-socle.html, json:build/reports/cucumber-rag-socle.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_rag_socle.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@rag-socle and not @integration")
class RagSocleCucumberRunner