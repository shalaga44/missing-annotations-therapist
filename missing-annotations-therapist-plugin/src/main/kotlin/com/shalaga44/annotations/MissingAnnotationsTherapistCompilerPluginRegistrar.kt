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
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.name.FqName

val KEY_FUNCTIONS = CompilerConfigurationKey<List<String>>("fully-qualified function names")
val KEY_ANNOTATION = CompilerConfigurationKey<List<String>>("fully-qualified annotation names")
val KEY_ANNOTATION_TARGET = CompilerConfigurationKey<List<String>>("fully-qualified annotation target names")
val KEY_PACKAGE_TARGET = CompilerConfigurationKey<List<String>>("fully-qualified package target names")
val KEY_SOURCE_SETS = CompilerConfigurationKey<List<String>>("fully-qualified source sets name")

@AutoService(CompilerPluginRegistrar::class)
class MissingAnnotationsTherapistCompilerPluginRegistrar(
  private val functions: Set<FqName>,
) : CompilerPluginRegistrar() {
  @Suppress("unused")
  constructor() : this(emptySet()) // Used by service loader

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val functions = configuration[KEY_FUNCTIONS]?.map { FqName(it) } ?: functions ?: emptyList()
    val annotation = configuration[KEY_ANNOTATION]?.map { FqName(it) } ?: emptyList()
    val packageTarget = configuration[KEY_PACKAGE_TARGET]?.map { FqName(it) } ?: emptyList()
    if (annotation.isEmpty()) return

    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    IrGenerationExtension.registerExtension(
      MissingAnnotationsTherapistIrGenerationExtension(
          messageCollector,
          functions.toSet(),
          annotations = annotation.toSet(),
        packageTargets=packageTarget.toSet(),
      ),
    )
  }
}
