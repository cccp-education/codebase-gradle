package codebase.scenarios

import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertRegistration
import codebase.koog.expert.ExpertRegistry
import codebase.koog.expert.ExpertExposureTask
import codebase.koog.expert.ExpertExposureManifest
import org.gradle.testfixtures.ProjectBuilder
import java.io.File

class Epic8ExpertExposureWorld {
    val registry = ExpertRegistry()
    val kotlinDomain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem")
    val docsDomain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing")

    var projectDir: File? = null
    var task: ExpertExposureTask? = null
    var taskGroup: String? = null
    var manifestContent: String? = null
    var parsedManifest: ExpertExposureManifest? = null
    var resolvedModel: String? = null
    var resolvedBaseUrl: String? = null
}
