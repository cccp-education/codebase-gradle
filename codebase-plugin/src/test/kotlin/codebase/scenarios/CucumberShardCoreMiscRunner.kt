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
 * CI shard — core/misc: vibecoding hardening (P1/P2), koog pipeline skeleton,
 * context preparation, expert dispatcher/exposure and the koog augmented
 * context planning (L-3).
 *
 * One of the exhaustive, disjoint feature shards (see
 * [CucumberShardEpic9Runner] for the rationale). Guarded by
 * `codebase.ci.CodebaseCucumberShardCoverageTest`.
 *
 * Note: this shard keeps the aggregate exclusion `not @integration` **without**
 * `not @wip` because `epic_7_expert_dispatcher.feature` and
 * `epic_8_expert_exposure.feature` are `@wip` at feature level yet are executed
 * in the current green CI through their dedicated `cucumberTestEpic7` /
 * `cucumberTestEpic8` runners (`filter = @epic7` / `@epic8`). Sharding preserves
 * that exact behaviour instead of silently dropping the two features.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/codebase_vibe_hardening.feature")
@SelectClasspathResource("features/codebase_vibe_hardening_2.feature")
@SelectClasspathResource("features/koog_pipeline_dedup.feature")
@SelectClasspathResource("features/prepare_context.feature")
@SelectClasspathResource("features/epic_7_expert_dispatcher.feature")
@SelectClasspathResource("features/epic_8_expert_exposure.feature")
@SelectClasspathResource("features/epic_l_3_augmented_planning.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/codebase_vibe_hardening.feature,src/test/features/codebase_vibe_hardening_2.feature," +
        "src/test/features/koog_pipeline_dedup.feature,src/test/features/prepare_context.feature," +
        "src/test/features/epic_7_expert_dispatcher.feature,src/test/features/epic_8_expert_exposure.feature," +
        "src/test/features/epic_l_3_augmented_planning.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "codebase.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-core-misc.html, json:build/reports/cucumber-shard-core-misc.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @integration")
class CucumberShardCoreMiscRunner
