package dev.shalaga44.mat

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.util.Base64

val KEY_CONFIG = CompilerConfigurationKey<String>("config")
val KEY_ENABLE_LOGGING = CompilerConfigurationKey<String>("enableLogging")

@Suppress("unused")
@AutoService(CommandLineProcessor::class)
class MissingAnnotationsTherapistCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = "io.github.shalaga44.missing-annotations-therapist"

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
      "config" -> {
        val decodedJson = String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        configuration.put(KEY_CONFIG, decodedJson)
      }
      else -> error("Unexpected config option ${option.optionName}")
    }
  }
}
