/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shalaga44.annotations

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@Suppress("unused")
@AutoService(CommandLineProcessor::class)
class MissingAnnotationsTherapistCommandLineProcessor : CommandLineProcessor {
  override val pluginId: String = "com.shalaga44.annotations.missing-annotations-therapist"

  override val pluginOptions: Collection<CliOption> = listOf(
    CliOption(
      optionName = "annotations",
      valueDescription = "fully-qualified annotations names",
      description = "fully-qualified annotations names",
      required = false, // TODO required for Kotlin/JS
      allowMultipleOccurrences = true,
    ),
    CliOption(
      optionName = "packagesTargets",
      valueDescription = "fully-qualified packages targets names",
      description = "fully-qualified packages targets names",
      required = false, // TODO required for Kotlin/JS
      allowMultipleOccurrences = true,
    ),
  )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ) {
    println("option = ${option}")
    println("option.optionName = ${option.optionName}")
    return when (option.optionName) {
      "annotations" -> configuration.add(KEY_ANNOTATIONS, value)
      "packagesTargets" -> configuration.add(KEY_PACKAGE_TARGETS, value)
      else -> error("Unexpected config option ${option.optionName}")
    }
  }
}
