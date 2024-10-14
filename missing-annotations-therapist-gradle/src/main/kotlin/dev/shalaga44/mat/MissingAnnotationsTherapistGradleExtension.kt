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

package dev.shalaga44.mat

/**
 * Represents an annotation to be added.
 *
 * @property fqName Fully qualified nameAsString of the annotation.
 * @property parameters Key-value pairs for annotation parameters. Supports dynamic templates.
 */
data class Annotation(
  val fqName: String,
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
)

/**
 * Specifies conditions under which annotations should be applied.
 *
 * @property existingAnnotations Annotations that must be present.
 * @property annotationsAbsence Annotations that must be absent.
 * @property visibility Visibility modifier required (PUBLIC, PRIVATE, etc.).
 * @property modifiers Additional modifiers required (OPEN, FINAL, etc.).
 * @property namePattern Regex pattern that the name must match.
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
 * @property superclass Fully qualified name of the required superclass.
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
  ABSTRACT,
  OPEN,
  FINAL,
  SEALED,
  DATA,
  ENUM,
  INTERFACE,
  OBJECT,
  COMPANION,
  CONST,
  LATEINIT,
  INLINE,
  NOINLINE,
  CROSSINLINE,
  REIFIED,
  TAILREC,
  SUSPEND,
  OPERATOR,
  INFIX,
  EXTERNAL,
  ANNOTATION,
  VARARG,
  OVERRIDE,
  EXPECT,
  ACTUAL,
  PUBLIC,
  PRIVATE,
  PROTECTED,
  INTERNAL
}

/**
 * Defines different types of class targets for annotations.
 */
enum class ClassTypeTarget {
  REGULAR_CLASS,          // Standard class declarations.
  ENUM_CLASS,             // Enum classes.
  SEALED_CLASS,           // Sealed classes.
  DATA_CLASS,             // Data classes.
  OBJECT_CLASS,           // Object declarations (singletons).
  ANNOTATION_CLASS,       // Annotation classes.
  INTERFACE_CLASS,        // Interface declarations.
  COMPANION_OBJECT_CLASS, // Companion objects within classes.
  LOCAL_CLASS,            // Classes declared within functions or local scopes.
  ANONYMOUS_CLASS,        // Anonymous classes (e.g., object expressions).
  ABSTRACT_CLASS,         // Abstract classes (regular classes with the 'abstract' modifier).
  OPEN_CLASS,             // Open classes (regular classes with the 'open' modifier).
  FINAL_CLASS,            // Final classes (regular classes with the 'final' modifier).
  INNER_CLASS,            // Inner classes within outer classes.
  NESTED_CLASS,           // Nested classes within outer classes.
  EXPECT_CLASS,           // Expect classes within outer classes.
  ACTUAL_CLASS,           // Actual classes within outer classes.
  INLINE_CLASS,           // inline (old) classes within outer classes.
  VALUE_CLASS,            // Value classes within outer classes.
}

/**
 * Defines different function types for annotations.
 */
enum class FunctionTypeTarget {
  FUNCTION,
  SUSPEND_FUNCTION,
  LAMBDA,
  CONSTRUCTOR,
}

/**
 * Defines different property targets for annotations.
 */
enum class PropertyTypeTarget {
  PROPERTY,
  FIELD,
  LOCAL_VARIABLE,
  VALUE_PARAMETER,
  GETTER,
  SETTER,
}

/**
 * Defines type alias targets for annotations.
 */
enum class TypeAliasTarget {
  TYPE_ALIAS,
}

/**
 * Defines file-level targets for annotations.
 */
enum class FileTarget {
  FILE,
}

/**
 * Represents an annotation addition rule.
 *
 * @property annotationsToAdd List of annotations to add.
 * @property classTargets List of class types to apply annotations to.
 * @property functionTargets List of function types to apply annotations to.
 * @property propertyTargets List of property types to apply annotations to.
 * @property typeAliasTargets List of type alias targets.
 * @property fileTargets List of file targets.
 * @property packageTarget List of package targets where annotations should be applied.
 * @property moduleTarget List of module targets to include or exclude.
 * @property sourceSets List of source sets to apply the annotations to.
 * @property conditions List of conditions that must be met to apply the annotations.
 * @property annotateNestedClassesNormallyInPackage If false, only top level classes in package will be annotated.
 * @property annotateNestedClassesRecursively If true, nested classes will also be annotated.
 * @property annotateFieldClassesRecursively If true, field-referenced classes will also be annotated.
 */
data class Annotate(
  var annotationsToAdd: List<Annotation>,
  var classTargets: List<ClassTypeTarget> = listOf(),
  var functionTargets: List<FunctionTypeTarget> = listOf(),
  var propertyTargets: List<PropertyTypeTarget> = listOf(),
  var typeAliasTargets: List<TypeAliasTarget> = listOf(),
  var fileTargets: List<FileTarget> = listOf(),
  var packageTarget: List<PackageTarget>,
  var moduleTarget: List<ModuleTarget> = listOf(),
  var sourceSets: List<String> = listOf(),
  var conditions: List<Condition> = listOf(),
  val annotateNestedClassesNormallyInPackage: Boolean = true,
  val annotateNestedClassesRecursively: Boolean = false,
  val annotateFieldClassesRecursively: Boolean = false,
)

/**
 * Gradle extension for configuring the MissingAnnotationsTherapist compiler plugin.
 */
open class MissingAnnotationsTherapist {
  var annotations: List<Annotate> = listOf()
  var enableLogging: Boolean = false
}
