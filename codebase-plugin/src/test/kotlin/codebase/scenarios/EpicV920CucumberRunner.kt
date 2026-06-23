package codebase.scenarios

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeTags
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasspathResource("features")
@IncludeTags("epic_v_9_20")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "codebase.scenarios")
class EpicV920CucumberRunner