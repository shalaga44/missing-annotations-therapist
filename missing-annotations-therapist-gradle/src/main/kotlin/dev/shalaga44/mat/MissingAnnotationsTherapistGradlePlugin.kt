@file:Suppress("unused")

package dev.shalaga44.mat

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.Base64

class MissingAnnotationsTherapistGradlePlugin : KotlinCompilerPluginSupportPlugin {

  companion object {
    const val COMPILER_PLUGIN_ID = "io.github.shalaga44.missing-annotations-therapist"
    const val PLUGIN_GROUP_ID = "io.github.shalaga44"
    const val PLUGIN_ARTIFACT_ID = "missing-annotations-therapist-plugin"
    const val PLUGIN_VERSION = "0.0.1"
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
    version = PLUGIN_VERSION,
  )

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension =
      project.extensions.findByName("kotlinMissingAnnotationsTherapist") as? MissingAnnotationsTherapistGradleExtension
        ?: return project.provider { emptyList() }

    val gson = GsonBuilder()
      .disableHtmlEscaping()
      .create()

    return project.provider {
      val configJson = gson.toJson(extension.annotations)

      val encodedJson = Base64.getEncoder().encodeToString(configJson.toByteArray(Charsets.UTF_8))

      project.logger.info("MissingAnnotationsTherapist Configuration: $configJson")
      listOf(
        SubpluginOption(
          key = "config",
          value = encodedJson,
        ),
      )
    }
  }
}
