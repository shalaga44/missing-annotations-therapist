plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("org.jmailen.kotlinter")
  `maven-publish`
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

/*
gradlePlugin {
  website.set("https://github.com/shalaga44/missing-annotations-therapist")
  vcsUrl.set("https://github.com/shalaga44/missing-annotations-therapist.git")

  plugins {
    create("kotlinMissingAnnotationsTherapist") {
      id = "com.shalaga44.annotations.missing-annotations-therapist"
      displayName = "Kotlin Missing Annotations Therapist Plugin"
      description = "Kotlin Compiler Plugin that adds missing annotations to your codebase."
      implementationClass = "com.shalaga44.annotations.MissingAnnotationsTherapistGradlePlugin"
      tags.set(listOf("kotlin", "annotations"))
    }
  }
}
*/

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
