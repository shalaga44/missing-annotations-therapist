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


data class Annotation(
  val fqName: String,
) {
  val shortName = fqName.substringAfterLast(".")
}

data class PackageTarget(
  val fqName: String,
) {
  val shortName = fqName.substringAfterLast(".")
}

data class Annotate(
  var annotationsToAdd: List<Annotation>,
  var annotationsTarget: List<kotlin.annotation.AnnotationTarget> = listOf(),
  var packageTarget: List<PackageTarget>,
  var sourceSets: List<String> = listOf(),
)

open class MissingAnnotationsTherapistGradleExtension {
  var annotations: List<Annotate> = listOf()
  var functions: List<String> = listOf("kotlin.assert")
  var excludedSourceSets: List<String> = listOf()
}
