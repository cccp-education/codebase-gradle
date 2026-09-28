package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@page-retrieval` scenarios
 * (EPIC CB-PAGE-PROVENANCE US-4, N1 scope — page provenance ingested with
 * a chunk, reaching the retrieval and the Docs channel BDD).
 *
 * Targets `page_retrieval.feature` and filters `@page-retrieval` tagged
 * scenarios (pattern S-082 — `CorpusIngestCucumberRunner`). No
 * `@integration` scenarios here — the domain is driven via the in-memory
 * [PageRetrievalFakeRagStore] and the real `CompositeContextBuilder`,
 * no network, no real pgvector, no Gradle task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-page-retrieval.html, json:build/reports/cucumber-page-retrieval.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/page_retrieval.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@page-retrieval and not @integration")
class PageRetrievalCucumberRunner
