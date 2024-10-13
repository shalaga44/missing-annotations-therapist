# missing-annotations-therapist

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.shalaga44.annotations/missing-annotations-therapist-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.shalaga44.annotations/missing-annotations-therapist-plugin)


> [!IMPORTANT]
> It's working now! but still a FIRST draft!,
> so think twice before using it be careful.


Kotlin Compiler Plugin that automatically adds missing annotations to your codebase based on configurable criteria. This
plugin supports all Kotlin platforms: JVM, JS, and Native, and utilizes the IR backend for the Kotlin compiler.

## Example

Given the following code:

```kotlin
package com.project.common.dto

class LoginRequestDto(
    val etc: String
)
```

With the `missing-annotations-therapist` plugin configured as follows:

```kotlin

configure<MissingAnnotationsTherapist> {
    annotations = listOf(
        Annotate(
            annotationsToAdd = listOf(Annotation(fqName = "kotlin.js.JsExport")),
            packageTarget = listOf(PackageTarget(pattern = "com.project.common.dto")),
            classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
            sourceSets = listOf("commonMain", "jsMain"),
        ),
    )
}
```

The transformed code will include the specified annotation:

```kotlin
package com.project.common.dto

@kotlin.js.JsExport
class LoginRequestDto(
    val etc: String
)
```

## Gradle Plugin

The Gradle plugin is available through the [Gradle Plugin Portal][missing-annotations-therapist-gradle].

```kotlin
plugins {
    kotlin("multiplatform") version "2.0.21"
    id("com.shalaga44.annotations.missing-annotations-therapist") version "0.1.0"
}
```
Also add the Github Maven repository to your repositories to be able to use it.

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/shalaga44/missing-annotations-therapist")
    }
}
```


Configure the plugin in your `build.gradle.kts` file:

```kotlin
import dev.shalaga44.mat.*

configure<MissingAnnotationsTherapist> {
    annotations = listOf(
        Annotate(
            annotationsToAdd = listOf(Annotation(fqName = "kotlin.js.JsExport")),
            classTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
            packageTarget = listOf(PackageTarget(pattern = "com.project.common.dto")),
            sourceSets = listOf("commonMain", "jsMain"),
        )
    )
}
```

For Groovy DSL:

```groovy
kotlinMissingAnnotationsTherapist {
    annotations = [
            Annotate(
                    annotationsToAdd = [Annotation("kotlin.js.JsExport")],
                    classTargets = [ClassTypeTarget.REGULAR_CLASS],
                    packageTarget = [PackageTarget("com.project.common.dto")],
                    sourceSets = ["commonMain", "jsMain"]
            )
    ]
}
```

## Compatibility

The Kotlin compiler plugin API is unstable, and each new version of Kotlin can bring breaking changes. Make sure you are
using the correct version of this plugin for your version of Kotlin. Check the table below for compatibility.

| Kotlin Version | Plugin Version |
|----------------|----------------|
| 2.0.21         | 0.1.0          |
| 2.1.0          | soon           |

## Kotlin IR

This plugin supports all IR-based compiler backends: JVM, JS, and Native. Ensure IR is enabled for Kotlin/JS by using
the following configuration:

```kotlin
target {
    js(IR) {
    }
}
```

Read the tests for use cases.

A working example is [[available]](https://github.com/shalaga44/missing-annotations-therapist/tree/main/sample) in this repository in the
sample directory.

