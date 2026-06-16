package codebase.koog.expert

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class CodebaseExpertExtension {
    abstract val domains: ListProperty<String>
    abstract val anonymizeEndpoints: Property<Boolean>
    abstract val outputFile: Property<String>
}
