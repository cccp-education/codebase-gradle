package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CI shard — OCR / RAG socle / fine-tuning / misc content (OCR pipeline and
 * ingest, page provenance retrieval, RAG socle, corpus ingest, doubt quality,
 * OCR contracts boundary, fine-tuning pipeline, anonymization, perception
 * benchmark, quality gate, STIMULUS cascade, blog narration).
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/ocr.feature")
@SelectClasspathResource("features/page_retrieval.feature")
@SelectClasspathResource("features/codebase_rag_socle.feature")
@SelectClasspathResource("features/codebase_corpus_ingest.feature")
@SelectClasspathResource("features/codebase_doubt_quality.feature")
@SelectClasspathResource("features/codebase_ocr_contracts.feature")
@SelectClasspathResource("features/codebase_finetuning.feature")
@SelectClasspathResource("features/epic_2_anonymization_expert.feature")
@SelectClasspathResource("features/epic_4_benchmark_perception.feature")
@SelectClasspathResource("features/epic_6_quality_gate.feature")
@SelectClasspathResource("features/epic_10_stimulus_cascade.feature")
@SelectClasspathResource("features/epic_12_blog_narration.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/ocr.feature,src/test/features/page_retrieval.feature," +
        "src/test/features/codebase_rag_socle.feature,src/test/features/codebase_corpus_ingest.feature," +
        "src/test/features/codebase_doubt_quality.feature,src/test/features/codebase_ocr_contracts.feature," +
        "src/test/features/codebase_finetuning.feature,src/test/features/epic_2_anonymization_expert.feature," +
        "src/test/features/epic_4_benchmark_perception.feature,src/test/features/epic_6_quality_gate.feature," +
        "src/test/features/epic_10_stimulus_cascade.feature,src/test/features/epic_12_blog_narration.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-ocr-rag.html, json:build/reports/cucumber-shard-ocr-rag.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration")
class CucumberShardOcrRagRunner
