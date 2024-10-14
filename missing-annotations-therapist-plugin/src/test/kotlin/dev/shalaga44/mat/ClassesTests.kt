package dev.shalaga44.mat

import com.tschuchort.compiletesting.SourceFile
import dev.shalaga44.mat.*
import org.junit.jupiter.api.Test

class ClassesTests {


  @Test
  fun `add annotation to regular class`() {
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
  fun `add annotation to enum class`() {
    val enumAnnotation = annotationFile("EnumAnnotation.kt", "com.project", "EnumAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            enum class Direction {
                NORTH, SOUTH, EAST, WEST
            }

            fun main() { 
                val annotations = Direction::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "EnumAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(enumAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.EnumAnnotation")),
                classTargets = listOf(ClassTypeTarget.ENUM_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to sealed class`() {
    val sealedAnnotation = annotationFile("SealedAnnotation.kt", "com.project", "SealedAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            sealed class Result {
                class Success(val data: String) : Result()
                class Error(val exception: Exception) : Result()
            }

            fun main() { 
                val annotations = Result::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "SealedAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(sealedAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.SealedAnnotation")),
                classTargets = listOf(ClassTypeTarget.SEALED_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to data class`() {
    val dataAnnotation = annotationFile("DataAnnotation.kt", "com.project", "DataAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            data class User(val name: String, val age: Int)

            fun main() { 
                val user = User("Alice", 30)
                val annotations = user::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "DataAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(dataAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.DataAnnotation")),
                classTargets = listOf(ClassTypeTarget.DATA_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to object class`() {
    val objectAnnotation = annotationFile("ObjectAnnotation.kt", "com.project", "ObjectAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            object Singleton {
                fun greet() = "Hello"
            }

            fun main() { 
                val annotations = Singleton::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "ObjectAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(objectAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.ObjectAnnotation")),
                classTargets = listOf(ClassTypeTarget.OBJECT_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to annotation class`() {
    val customAnnotation = annotationFile("CustomAnnotation.kt", "com.project", "CustomAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            @CustomAnnotation
            annotation class JsonSerializable

            fun main() { 
                val annotations = JsonSerializable::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "CustomAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(customAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.CustomAnnotation")),
                classTargets = listOf(ClassTypeTarget.ANNOTATION_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to interface class`() {
    val interfaceAnnotation = annotationFile("InterfaceAnnotation.kt", "com.project", "InterfaceAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            interface Drivable

            fun main() { 
                val annotations = Drivable::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "InterfaceAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(interfaceAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.InterfaceAnnotation")),
                classTargets = listOf(ClassTypeTarget.INTERFACE_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to companion object class`() {
    val companionAnnotation = annotationFile("CompanionAnnotation.kt", "com.project", "CompanionAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            class MyClass {
                companion object {
                    fun create() = MyClass()
                }
            }

            fun main() { 
                val companionAnnotations = MyClass.Companion::class.annotations
                assertTrue(companionAnnotations.any { it.annotationClass.simpleName == "CompanionAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(companionAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.CompanionAnnotation")),
                classTargets = listOf(ClassTypeTarget.COMPANION_OBJECT_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to local class`() {
    val localClassAnnotation = annotationFile("LocalClassAnnotation.kt", "com.project", "LocalClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            fun createLocalClass() {
                class LocalHelper
                // The annotation should be applied to LocalHelper
                val helper = LocalHelper()
                val annotations = helper::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "LocalClassAnnotation" })
            }

            fun main() {
                createLocalClass()
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(localClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.LocalClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.LOCAL_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }


  @Test
  fun `add annotation to anonymous class`() {
    val anonymousClassAnnotation =
      annotationFile("AnonymousClassAnnotation.kt", "com.project", "AnonymousClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            fun createAnonymousObject() {
                val obj = object {
                    // The annotation should be applied to this anonymous object
                }
                val annotations = obj::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "AnonymousClassAnnotation" })
            }

            fun main() {
                createAnonymousObject()
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(anonymousClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.AnonymousClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.ANONYMOUS_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to abstract class`() {
    val abstractAnnotation = annotationFile("AbstractClassAnnotation.kt", "com.project", "AbstractClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            abstract class Shape

            fun main() { 
                val annotations = Shape::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "AbstractClassAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(abstractAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.AbstractClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.ABSTRACT_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to open class`() {
    val openClassAnnotation = annotationFile("OpenClassAnnotation.kt", "com.project", "OpenClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            open class Vehicle

            class Car : Vehicle()

            fun main() { 
                val annotations = Vehicle::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "OpenClassAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(openClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.OpenClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.OPEN_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to final class`() {
    val finalClassAnnotation = annotationFile("FinalClassAnnotation.kt", "com.project", "FinalClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            final class Immutable

            fun main() { 
                val annotations = Immutable::class.annotations
                assertTrue(annotations.any { it.annotationClass.simpleName == "FinalClassAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(finalClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.FinalClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.FINAL_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }

  @Test
  fun `add annotation to inner class`() {
    val innerClassAnnotation = annotationFile("InnerClassAnnotation.kt", "com.project", "InnerClassAnnotation")

    val mainSource = """
            package com.project
            import kotlin.test.assertTrue

            class Outer {
                inner class Inner
            }

            fun main() { 
                val innerAnnotations = Outer.Inner::class.annotations
                assertTrue(innerAnnotations.any { it.annotationClass.simpleName == "InnerClassAnnotation" })
            }
        """.trimIndent()

    run(
      mainSource = mainSource,
      additionalSources = listOf(innerClassAnnotation),
      mainApplication = "com.project.MainKt",
      compilerPluginRegistrars = arrayOf(
        MissingAnnotationsTherapistCompilerPluginRegistrar(
          MissingAnnotationsTherapistArgs(
            annotations = listOf(
              Annotate(
                annotationsToAdd = listOf(Annotation(fqName = "com.project.InnerClassAnnotation")),
                classTargets = listOf(ClassTypeTarget.INNER_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
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
                class Nested
            }

            fun main() { 
                val nestedAnnotations = Outer.Nested::class.annotations
                assertTrue(nestedAnnotations.any { it.annotationClass.simpleName == "NestedClassAnnotation" })
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
                classTargets = listOf(ClassTypeTarget.NESTED_CLASS),
                packageTarget = listOf(PackageTarget(pattern = "com.project")),
              ),
            ),
          ),
        ),
      ),
    )
  }



}

fun annotationFile(
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
