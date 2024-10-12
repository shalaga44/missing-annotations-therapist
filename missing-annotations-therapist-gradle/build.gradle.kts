plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("org.jmailen.kotlinter")
  `java-gradle-plugin`
  `maven-publish`
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
  implementation("com.google.code.gson:gson:2.8.9")
}
tasks.named("lintBuildscripts").configure {
  enabled = false
}
//tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask> {
//  // Explicit dependencies for the lintBuildscripts task
//  mustRunAfter(tasks.named("compileKotlin"))
//  mustRunAfter(tasks.named("generateBuildConfig"))
//  mustRunAfter(tasks.named("processResources"))
//  mustRunAfter(tasks.named("pluginDescriptors"))
//}
buildConfig {
  val project = project(":missing-annotations-therapist-plugin")
  packageName(project.group.toString())
  buildConfigField("String", "PLUGIN_GROUP_ID", "\"${project.group}\"")
  buildConfigField("String", "PLUGIN_ARTIFACT_ID", "\"${project.name}\"")
  buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
  website.set("https://github.com/shalaga44/missing-annotations-therapist")
  vcsUrl.set("https://github.com/shalaga44/missing-annotations-therapist.git")

  plugins {
    create("kotlinMissingAnnotationsTherapist") {
      id = "io.github.shalaga44.missing-annotations-therapist"
      implementationClass = "dev.shalaga44.mat.MissingAnnotationsTherapistGradlePlugin"
      displayName = "Kotlin Missing Annotations Therapist Plugin"
      description = "Kotlin Compiler Plugin that adds missing annotations to your codebase."
      tags.set(listOf("kotlin", "annotations"))
    }
  }
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/shalaga44/missing-annotations-therapist")
      credentials {
        username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USERNAME")
        password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
      }
    }

    maven {
      name = "localMaven"
      url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
    }
  }
}
