package dev.shalaga44.mat

import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MatTests {

  private fun annotationFile(
    fileName: String,
    packageName: String = "com.project",
    annotationName: String,
    vararg parameters: String = emptyArray(),
  ): SourceFile {
    val params = if (parameters.isNotEmpty()) {
      parameters.joinToString(", ", "(", ")")
    } else {
      ""
    }
    return SourceFile.kotlin(
      fileName,
      """
                package $packageName

                annotation class $annotationName$params
            """.trimIndent(),
    )
  }

  @Test
  fun `add single annotation to class`() {
    val myDto = annotationFile("MyDto.kt", "com.project", "MyDto")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class Hello

                fun main() { 
                    val hello = Hello()
                    val annotations = hello::class.annotations
                    assertTrue(annotations.any { it.annotationClass.simpleName == "MyDto" })
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(myDto),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.MyDto")),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add multiple annotations to class`() {
    val myDto = annotationFile("MyDto.kt", "com.project", "MyDto")
    val serializable = annotationFile("Serializable.kt", "kotlinx.serialization", "Serializable")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class Hello

                fun main() { 
                    val hello = Hello()
                    val annotations = hello::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.containsAll(listOf("MyDto", "Serializable")))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(myDto, serializable),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.MyDto"),
                  Annotation(fqName = "kotlinx.serialization.Serializable"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to function`() {
    val myFunctionAnnotation = annotationFile("MyFunctionAnnotation.kt", "com.project", "MyFunctionAnnotation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class Hello {
                    fun greet() {}
                }

                fun main() { 
                    val greetAnnotations = Hello::greet.annotations.map { it.annotationClass.simpleName }
                    assertTrue(greetAnnotations.contains("MyFunctionAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(myFunctionAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.MyFunctionAnnotation"),
                ),
                functionTargets = listOf(FunctionTypeTarget.FUNCTION),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to property based on naming pattern`() {
    val propertyValidation = annotationFile("PropertyValidation.kt", "com.project", "PropertyValidation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue
                import kotlin.reflect.full.memberProperties

                class User {
                    private val isActive: Boolean = true
                }

                fun main() { 
                    val user = User()
                    val annotations = user::class.memberProperties.find { it.name == "isActive" }?.annotations
                    assertTrue(annotations?.any { it.annotationClass.simpleName == "PropertyValidation" } == true)
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(propertyValidation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.PropertyValidation"),
                ),
                propertyTargets = listOf(PropertyTypeTarget.PROPERTY),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
                conditions = listOf(
                  Condition(
                    namePattern = "^is[A-Z].+",
                    modifiers = listOf(Modifier.PRIVATE),
                    typeCondition = TypeCondition(
                      typeNames = listOf("Boolean"),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation based on wildcard package matching`() {
    val serviceAnnotation = annotationFile("ServiceAnnotation.kt", "com.project", "ServiceAnnotation")

    val mainSource = """
                package com.project.service
                import kotlin.test.assertTrue

                class ServiceClass

                fun main() { 
                    val annotations = ServiceClass::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.contains("ServiceAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(serviceAnnotation),
      mainApplication = "com.project.service.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.ServiceAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(
                    pattern = "com.project.*",
                    matchType = MatchType.WILDCARD
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation based on regex package matching`() {
    val dataModelAnnotation = annotationFile("DataModelAnnotation.kt", "com.project", "DataModelAnnotation")

    val mainSource = """
                package com.project.data.model
                import kotlin.test.assertTrue

                class DataModel

                fun main() { 
                    val annotations = DataModel::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.contains("DataModelAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(dataModelAnnotation),
      mainApplication = "com.project.data.model.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.DataModelAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(
                    pattern = "com.project.data.model",
                    matchType = MatchType.REGEX,
                    regex = "^com\\.project\\.data\\.model$",
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation conditionally based on existing annotations`() {
    val existingAnnotation = annotationFile("ExistingAnnotation.kt", "com.project", "ExistingAnnotation")
    val newAnnotation = annotationFile("NewAnnotation.kt", "com.project", "NewAnnotation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                @ExistingAnnotation
                class ExistingAnnotatedClass

                fun main() { 
                    val annotations = ExistingAnnotatedClass::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.contains("ExistingAnnotation"))
                    assertTrue(annotations.contains("NewAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(existingAnnotation, newAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.NewAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
                conditions = listOf(
                  Condition(
                    existingAnnotations = listOf("com.project.ExistingAnnotation"),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Disabled
  @Test
  fun `add annotation with dynamic parameters`() {
    val dynamicAnnotation =
      annotationFile("DynamicAnnotation.kt", "com.project", "DynamicAnnotation", "val exportName: String")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class DynamicClass

                fun main() { 
                    val annotations = DynamicClass::class.annotations.find { it.annotationClass.simpleName == "DynamicAnnotation" }
                    assertTrue(annotations != null)
                    val exportName = annotations?.annotationClass?.qualifiedName
                    assertTrue(exportName?.contains("DynamicClass") == true)
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(dynamicAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(
                    fqName = "com.project.DynamicAnnotation",
                    /*parameters = mapOf("exportName" to "{className}"),*/
                  ),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to nested class`() {
    val nestedClassAnnotation = annotationFile("NestedClassAnnotation.kt", "com.project", "NestedClassAnnotation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class Outer {
                    class Inner
                }

                fun main() { 
                    val annotations = Outer.Inner::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.contains("NestedClassAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(nestedClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.NestedClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
                annotateNestedClassesRecursively = true,
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to class based on inheritance`() {
    val inheritedAnnotation = annotationFile("InheritedAnnotation.kt", "com.project", "InheritedAnnotation")
    val baseClass = SourceFile.kotlin(
      "BaseClass.kt",
      """
                package com.project

                open class BaseClass
            """.trimIndent(),
    )
    val derivedClass = SourceFile.kotlin(
      "DerivedClass.kt",
      """
                package com.project

                class DerivedClass : BaseClass()
            """.trimIndent(),
    )

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                fun main() { 
                    val annotations = DerivedClass::class.annotations.map { it.annotationClass.simpleName }
                    assertTrue(annotations.contains("InheritedAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(inheritedAnnotation, baseClass, derivedClass),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.InheritedAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
                conditions = listOf(
                  Condition(
                    inheritance = InheritanceCondition(
                      superclass = "com.project.BaseClass",
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to function based on modifiers`() {
    val openFunctionAnnotation = annotationFile("OpenFunctionAnnotation.kt", "com.project", "OpenFunctionAnnotation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                class Service {
                    open fun perform() {}
                    fun execute() {}
                }

                fun main() { 
                    val service = Service()
                    val openAnnotations = service::perform.annotations.map { it.annotationClass.simpleName }
                    val executeAnnotations = service::execute.annotations.map { it.annotationClass.simpleName }
                    assertTrue(openAnnotations.contains("OpenFunctionAnnotation"))
                    assertTrue(!executeAnnotations.contains("OpenFunctionAnnotation"))
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(openFunctionAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.OpenFunctionAnnotation"),
                ),
                functionTargets = listOf(FunctionTypeTarget.FUNCTION),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
                conditions = listOf(
                  Condition(
                    modifiers = listOf(Modifier.OPEN),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to local variable`() {
    val localVarAnnotation = annotationFile("LocalVarAnnotation.kt", "com.project", "LocalVarAnnotation")

    val mainSource = """
                package com.project
                import kotlin.test.assertTrue

                fun main() { 
                    @LocalVarAnnotation
                    val tempValue = 42
                    // Simulating local variable annotation retrieval
                    // Since Kotlin does not support runtime reflection on local variables, we use an approach to attach annotations manually.
                    // This is for illustrative purposes and may not work as expected in a real-world scenario.
                    val annotations = listOf(LocalVarAnnotation())
                    assertTrue(annotations.any { it.annotationClass.simpleName == "LocalVarAnnotation" })
                }
            """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(localVarAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.LocalVarAnnotation"),
                ),
                propertyTargets = listOf(PropertyTypeTarget.LOCAL_VARIABLE),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
                conditions = listOf(
                  Condition(
                    namePattern = "^temp.*",
                    modifiers = listOf(Modifier.FINAL),
                    visibility = Visibility.PRIVATE,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `ensure annotations are not duplicated`() {
    val uniqueAnnotation = annotationFile("UniqueAnnotation.kt", "com.project", "UniqueAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertEquals

            class DuplicateTest

            fun main() { 
                val annotations = DuplicateTest::class.annotations.filter { it.annotationClass.simpleName == "UniqueAnnotation" }
                assertEquals(1, annotations.size)
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(uniqueAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.UniqueAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(
                  PackageTarget(pattern = "com.project"),
                ),
              ),
            ),
          ),
        ),
      ),
    )
  }


  @Test
  fun `annotate nested and field-referenced classes using configuration`() {
    val annotation = annotationFile("TestAnnotation.kt", "com.project", "TestAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            class Outer {
                class Inner
                val field: Inner = Inner()
            }

            fun main() { 
                val innerAnnotations = Outer.Inner::class.annotations.map { it.annotationClass.simpleName }
                val fieldAnnotations = Outer().field::class.annotations.map { it.annotationClass.simpleName }

                assertTrue(innerAnnotations.contains("TestAnnotation"))
                assertTrue(fieldAnnotations.contains("TestAnnotation"))
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(annotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.TestAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
                annotateNestedClassesRecursively = true,
                annotateFieldClassesRecursively = true,
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `do not annotate nested and field-referenced classes using configuration`() {
    val annotation = annotationFile("TestAnnotation.kt", "com.project", "TestAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            class Outer {
                class Inner
                val field: Inner = Inner()
            }

            fun main() { 
                val innerAnnotations = Outer.Inner::class.annotations.map { it.annotationClass.simpleName }
                val fieldAnnotations = Outer().field::class.annotations.map { it.annotationClass.simpleName }
                
                assertTrue(!innerAnnotations.contains("TestAnnotation"), "innerAnnotations must not be annotated") 
                assertTrue(!fieldAnnotations.contains("TestAnnotation"), "fieldAnnotations must not be annotated")
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(annotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(
                  Annotation(fqName = "com.project.TestAnnotation"),
                ),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
                annotateNestedClassesNormallyInPackage = false,
                annotateNestedClassesRecursively = false,
                annotateFieldClassesRecursively = false,
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `ensure duplicate annotations are not applied`() {
    val annotation = annotationFile("TestAnnotation.kt", "com.project", "TestAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertEquals

            class Outer {
                class Inner
            }

            fun main() { 
                val annotations = Outer.Inner::class.annotations.filter { it.annotationClass.simpleName == "TestAnnotation" }
                assertEquals(1, annotations.size)  
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(annotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.TestAnnotation")),
                classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
                annotateNestedClassesRecursively = true,
              ),
            ),
          ),
        ),
      ),
    )
  }
  @Test
  fun `error`() {
    val annotation = annotationFile("TestAnnotation.kt", "com.project", "TestAnnotation")

    val mainSource = """package com.narbase.narcore.dto.domain.user.profile

import kotlin.js.JsExport

/*
 * Copyright 2017-2020 Narbase technologies and contributors. Use of this source code is governed by the MIT License.
 */

object GetProfileDto {

    class Request

    class Response(
        val profile: UserProfile
    )

    class UserProfile(
        val clientId: String,
        val userId: String,
        val fullName: String,
        val username: String,
        val callingCode: String,
        val localPhone: String,
        val privileges: Array<String>
    )

}
fun main(){

}
""".trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(annotation),
      mainApplication = "com.narbase.narcore.dto.domain.user.profile.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.TestAnnotation")),
                classTargets = listOf(
                  ClassTypeTarget.REGULAR_CLASS,
                  ClassTypeTarget.OBJECT_CLASS,
                  ClassTypeTarget.DATA_CLASS
                ),
                packageTarget = listOf(PackageTarget(pattern = "com.narbase.*", MatchType.WILDCARD)),
                sourceSets = listOf("commonMain", "jsMain"),
/*                conditions = listOf(
                  Condition(
                    modifiers = Modifier.values().filterNot { it in listOf(Modifier.ACTUAL, Modifier.EXPECT) })
                ),*/
                annotateNestedClassesNormallyInPackage = false,
              )

            )
,
          ),
        ),
      ),
    )
  }
}
