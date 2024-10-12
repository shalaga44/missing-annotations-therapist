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

package com.shalaga44.annotations.utils

/**
 * Represents an annotation to be added.
 *
 * @property fqName Fully qualified nameAsString of the annotation.
 * @property parameters Key-value pairs for annotation parameters. Supports dynamic templates.
 */
data class Annotation(
  val fqName: String,
  val parameters: Map<String, String> = emptyMap(),
) {
  val shortName: String = fqName.substringAfterLast(".")
}

/**
 * Defines the target packages where annotations should be applied.
 *
 * @property pattern The package pattern to match.
 * @property matchType The type of matching (EXACT, WILDCARD, REGEX).
 * @property regex Optional regex pattern for complex matching. Required if matchType is REGEX.
 */
data class PackageTarget(
  val pattern: String,
  val matchType: MatchType = MatchType.EXACT,
  val regex: String? = null,
) {
  val shortName: String = pattern.substringAfterLast(".")
}

/**
 * Specifies conditions under which annotations should be applied.
 *
 * @property existingAnnotations Annotations that must be present.
 * @property annotationsAbsence Annotations that must be absent.
 * @property visibility Visibility modifier required (PUBLIC, PRIVATE, etc.).
 * @property modifiers Additional modifiers required (OPEN, FINAL, etc.).
 * @property namePattern Regex pattern that the nameAsString must match.
 * @property inheritance Conditions based on superclass and interfaces.
 * @property typeCondition Conditions based on type names (for properties).
 * @property customPredicate Custom logic for complex conditions.
 */
data class Condition(
  val existingAnnotations: List<String> = emptyList(),
  val annotationsAbsence: List<String> = emptyList(),
  val visibility: Visibility? = null,
  val modifiers: List<Modifier> = emptyList(),
  val namePattern: String? = null,
  val inheritance: InheritanceCondition? = null,
  val typeCondition: TypeCondition? = null,
  val customPredicate: String? = null,
)

/**
 * Defines inheritance-based conditions.
 *
 * @property superclass Fully qualified nameAsString of the required superclass.
 * @property interfaces List of fully qualified names of required interfaces.
 */
data class InheritanceCondition(
  val superclass: String? = null,
  val interfaces: List<String> = emptyList(),
)

/**
 * Defines type-based conditions for properties.
 *
 * @property typeNames List of type names that the property must match.
 */
data class TypeCondition(
  val typeNames: List<String> = emptyList(),
)

/**
 * Specifies module targets within a multi-module project.
 *
 * @property moduleName Name of the module to target.
 * @property inclusion If true, includes the module; if false, excludes it.
 */
data class ModuleTarget(
  val moduleName: String,
  val inclusion: Boolean = true,
)

/**
 * Defines the type of package matching.
 */
enum class MatchType {
  EXACT,
  WILDCARD,
  REGEX
}

/**
 * Defines visibility modifiers for conditions.
 */
enum class Visibility {
  PUBLIC,
  PRIVATE,
  PROTECTED,
  INTERNAL
}

/**
 * Defines additional modifiers for conditions.
 */
enum class Modifier {
  OPEN,
  FINAL,
  ABSTRACT,
  SUSPEND,
  PRIVATE,
}

/**
 * Represents an annotation addition rule.
 *
 * @property annotationsToAdd List of annotations to add.
 * @property annotationsTarget List of Kotlin annotation targets (CLASS, FUNCTION, PROPERTY, etc.).
 * @property packageTarget List of package targets where annotations should be applied.
 * @property moduleTarget List of module targets to include or exclude.
 * @property sourceSets List of source sets to apply the annotations to.
 * @property conditions List of conditions that must be met to apply the annotations.
 */
data class Annotate(
  var annotationsToAdd: List<Annotation>,
  var annotationsTarget: List<AnnotationTarget> = listOf(),
  var packageTarget: List<PackageTarget>,
  var moduleTarget: List<ModuleTarget> = listOf(),
  var sourceSets: List<String> = listOf(),
  var conditions: List<Condition> = listOf(),
)

/**
 * Gradle extension for configuring the MissingAnnotationsTherapist compiler plugin.
 */
open class MissingAnnotationsTherapistGradleExtension {
  var annotations: List<Annotate> = listOf()
}
