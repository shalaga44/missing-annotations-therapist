import com.shalaga44.annotations.Annotate
import com.shalaga44.annotations.Annotation
import com.shalaga44.annotations.MissingAnnotationsTherapistGradleExtension
import com.shalaga44.annotations.PackageTarget

plugins {
  kotlin("multiplatform") version "1.8.20"
  id("com.shalaga44.annotations.missing-annotations-therapist") version "0.1.0-SNAPSHOT"
}

repositories {
  mavenLocal()
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
      annotationsToAdd = listOf(Annotation("com.project.common.dto.MyDto")),
      annotationsTarget = listOf(AnnotationTarget.CLASS),
      packageTarget = listOf(PackageTarget("com.project.common.dto")),
      sourceSets = listOf("commonMain","jvmMain", "jsMain", "nativeMain"),
    ),
  )
}
