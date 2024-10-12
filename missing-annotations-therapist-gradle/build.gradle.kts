plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.gradle.plugin-publish")
  id("com.github.gmazzo.buildconfig")
  id("org.jmailen.kotlinter")
}

dependencies {
  
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
  implementation("com.google.code.gson:gson:2.8.9")
}

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
      id = "com.shalaga44.annotations.missing-annotations-therapist"
      displayName = "Kotlin Missing Annotations Therapist Plugin"
      description = "Kotlin Compiler Plugin that automatically adds missing annotations to your codebase based on configurable criteria"
      implementationClass = "com.shalaga44.annotations.MissingAnnotationsTherapistGradlePlugin"
      tags.set(listOf("kotlin", " missing-annotations-therapist", "annotations"))
    }
  }
}

tasks.named("publish") {
  dependsOn("publishPlugins")
}

publishing {
  repositories {
    maven {
      name = "test"
      url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
    }
  }
}
