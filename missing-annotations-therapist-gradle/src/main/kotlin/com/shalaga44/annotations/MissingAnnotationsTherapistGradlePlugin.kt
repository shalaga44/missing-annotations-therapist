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
    val currentSourceSet = kotlinCompilation.defaultSourceSet.name
    return extension.annotations.map { it.sourceSets }.flatten().any { it == currentSourceSet }
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
        System.err.println("annotationsToAdd = ${it}")
        SubpluginOption(key = "annotations", value = it)
      } +
        extension.annotations.map { it.packageTarget.map { it.fqName } }.flatten().map {
          System.err.println("packageTargets = ${it}")
          SubpluginOption(key = "packagesTargets", value = it)
        }
    }
  }
}
