package com.shalaga44.annotations

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class MissingAnnotationsTherapistGradlePlugin : KotlinCompilerPluginSupportPlugin {

  companion object {
    const val COMPILER_PLUGIN_ID = "com.shalaga44.annotations.missing-annotations-therapist"
    const val PLUGIN_GROUP_ID = "com.shalaga44.annotations"
    const val PLUGIN_ARTIFACT_ID = "missing-annotations-therapist-plugin"
    const val PLUGIN_VERSION = "1.0.0"
  }

  override fun apply(target: Project): Unit = with(target) {
    extensions.create("kotlinMissingAnnotationsTherapist", MissingAnnotationsTherapistGradleExtension::class.java)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.findByName("kotlinMissingAnnotationsTherapist") as? MissingAnnotationsTherapistGradleExtension ?: return false
    val currentSourceSet = kotlinCompilation.defaultSourceSet.name
    return extension.annotations.any { it.sourceSets.contains(currentSourceSet) }
  }

  override fun getCompilerPluginId(): String = COMPILER_PLUGIN_ID

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = PLUGIN_GROUP_ID,
    artifactId = PLUGIN_ARTIFACT_ID,
    version = PLUGIN_VERSION
  )

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.findByName("kotlinMissingAnnotationsTherapist") as? MissingAnnotationsTherapistGradleExtension ?: return project.provider { emptyList() }

    val gson: Gson = GsonBuilder().create()

    return project.provider {
      val configJson = gson.toJson(extension.annotations)
      project.logger.info("MissingAnnotationsTherapist Configuration: $configJson")
      listOf(
        SubpluginOption(key = "config", value = configJson)
      )
    }
  }
}
