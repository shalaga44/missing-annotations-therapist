package com.shalaga44.annotations

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused")
class MissingAnnotationsTherapistGradlePlugin : KotlinCompilerPluginSupportPlugin {
  override fun apply(target: Project): Unit = with(target) {
    extensions.create("kotlinMissingAnnotationsTherapist", MissingAnnotationsTherapistGradleExtension::class.java)
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(MissingAnnotationsTherapistGradleExtension::class.java)
    return extension.excludedSourceSets.none { it == kotlinCompilation.defaultSourceSet.name }
  }

  override fun getCompilerPluginId(): String = "com.shalaga44.annotations.missing-annotations-therapist"

  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = BuildConfig.PLUGIN_GROUP_ID,
    artifactId = BuildConfig.PLUGIN_ARTIFACT_ID,
    version = BuildConfig.PLUGIN_VERSION,
  )

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>,
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(MissingAnnotationsTherapistGradleExtension::class.java)
    return project.provider {

      extension.annotations.map { it.annotationsToAdd.map { it.fqName } }.flatten().map {
        SubpluginOption(key = "annotation", value = it)
      }
      extension.annotations.map { it.packageTarget.map { it.fqName } }.flatten().map {
        SubpluginOption(key = "packageTarget", value = it)
      }
    }
  }
}
