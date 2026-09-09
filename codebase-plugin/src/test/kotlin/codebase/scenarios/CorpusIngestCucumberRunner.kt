package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.*
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Cucumber runner dedicated to `@corpus-ingest` scenarios
 * (EPIC CB-FPA-RAG US-3 — corpus loading, doubt policy, ingestion
 * contract, Docs exposure BDD).
 *
 * Targets `codebase_corpus_ingest.feature` and filters `@corpus-ingest`
 * tagged scenarios (pattern S-082 — `DoubtQualityCucumberRunner`). No
 * `@integration` scenarios here — the domain is driven via fakes/stubs
 * (`CorpusFakeRagStore`, `CorpusStubRagStore`), no network, no real
 * pgvector, no Gradle task.
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-corpus-ingest.html, json:build/reports/cucumber-corpus-ingest.json"
)
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/codebase_corpus_ingest.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@corpus-ingest and not @integration")
class CorpusIngestCucumberRunner