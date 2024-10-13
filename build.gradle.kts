import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.KotlinterPlugin
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
  kotlin("jvm") version "2.0.21" apply false
  kotlin("multiplatform") version "2.0.21" apply false
  id("com.github.gmazzo.buildconfig") version "3.1.0" apply false
  id("org.jmailen.kotlinter") version "3.14.0" apply false
  `maven-publish` // Added for publishing to GitHub Packages
}

allprojects {
  group = "io.github.shalaga44"
  version = "0.0.2"

  repositories {
    mavenCentral()
    maven {
      url = uri("https://maven.pkg.github.com/shalaga44/missing-annotations-therapist")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USERNAME")
        password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
      }
    }
  }

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  plugins.withType<KotlinterPlugin> {
    val formatBuildscripts = tasks.register<FormatTask>("formatBuildscripts") {
      group = "verification"
      source(layout.projectDirectory.asFileTree.matching { include("**/*.kts") })
    }
    tasks.named("formatKotlin") { dependsOn(formatBuildscripts) }

    val lintBuildscripts = tasks.register<LintTask>("lintBuildscripts") {
      group = "verification"
      source(layout.projectDirectory.asFileTree.matching { include("**/*.kts") })
    }
    tasks.named("lintKotlin") { dependsOn(lintBuildscripts) }
  }
}
