import dev.shalaga44.mat.*

plugins {
  id("io.github.shalaga44.missing-annotations-therapist") version "0.1.0"
  kotlin("multiplatform") version "2.0.21"
}

repositories {
  mavenLocal()
  maven {
    url = uri("https://maven.pkg.github.com/shalaga44/missing-annotations-therapist")
  }
  maven {
    url = uri("~/.m2/repository")
  }
  mavenCentral()
}

kotlin {
  jvm()
  js(IR) {
    browser()
    nodejs()
  }

  val osName = System.getProperty("os.name")
  val osArch = System.getProperty("os.arch")
  when {
    "Windows" in osName -> mingwX64("native")
    "Mac OS" in osName -> when (osArch) {
      "aarch64" -> macosArm64("native")
      else -> macosX64("native")
    }

    else -> linuxX64("native")
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("reflect"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit5"))
      }
    }
    val jsMain by getting {
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }
    val nativeMain by getting {
      dependsOn(commonMain)
    }
    val nativeTest by getting {
      dependsOn(commonTest)
    }
  }

  // TODO Kotlin/JS loses class information at the IR level -> Soft-assertion doesn't work
  // targets.all {
  //   compilations.all {
  //     kotlinOptions.languageVersion = "2.0"
  //   }
  // }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

configure<MissingAnnotationsTherapistGradleExtension> {
  annotations = listOf(
    Annotate(
      annotationsToAdd = listOf(Annotation("kotlin.js.JsExport")),
      classesclassTargets = listOf(ClassTypeTarget.REGULAR_CLASS),
      packageTarget = listOf(PackageTarget("com.project.common.dto.js")),
      sourceSets = listOf( "jsMain", "jsTest", ),

    ),
  )
}
