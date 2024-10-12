package dev.shalaga44.mat

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

val KEY_CONFIG = CompilerConfigurationKey<String>("config")
val KEY_ENABLE_LOGGING = CompilerConfigurationKey<String>("enableLogging")

@Suppress("unused")
@AutoService(CommandLineProcessor::class)
class MissingAnnotationsTherapistCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = "com.shalaga44.annotations.missing-annotations-therapist"

  override val pluginOptions: Collection<CliOption> = listOf(
      CliOption(
          optionName = "config",
          valueDescription = "JSON configuration for MissingAnnotationsTherapist",
          description = "JSON configuration for MissingAnnotationsTherapist compiler plugin",
          required = true,
          allowMultipleOccurrences = false,
      ),
  )

  override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    when (option.optionName) {
      "config" -> configuration.put(KEY_CONFIG, value)
      else -> error("Unexpected config option ${option.optionName}")
    }
  }
}
