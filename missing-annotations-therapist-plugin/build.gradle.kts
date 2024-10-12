import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("kapt")
  signing
  `maven-publish`
  id("org.jmailen.kotlinter")
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  kapt("com.google.auto.service:auto-service:1.0.1")
  compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")
  implementation("com.google.code.gson:gson:2.8.9")

  testImplementation(kotlin("test-junit5"))
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("dev.zacsweers.kctfork:core:0.5.1")
  testImplementation(enforcedPlatform("org.junit:junit-bom:5.9.1"))
}

tasks.named("lintBuildscripts").configure {
  enabled = false
}

tasks.named("lintKotlinMain").configure {
  enabled = false
}
tasks.named("lintKotlinTest").configure {
  enabled = false
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
  kotlinOptions.freeCompilerArgs += listOf(
    "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
  )
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.register("sourcesJar", Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

signing {
  setRequired(provider { gradle.taskGraph.hasTask("publish") })
  sign(publishing.publications)
}

publishing {
  publications {
    create<MavenPublication>("default") {
      from(components["java"])
      artifact(tasks["sourcesJar"])

      pom {
        name.set(project.name)
        description.set("Kotlin Compiler Plugin that automatically adds missing annotations to your codebase.")
        url.set("https://github.com/shalaga44/missing-annotations-therapist")

        licenses {
          license {
            name.set("Apache License 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        }
        developers {
          developer {
            id.set("shalaga44")
            name.set("Shalaga44")
            email.set("shalaga44@e.email")
          }
        }
        scm {
          connection.set("scm:git:git://github.com/shalaga44/missing-annotations-therapist.git")
          developerConnection.set("scm:git:ssh://github.com/shalaga44/missing-annotations-therapist.git")
          url.set("https://github.com/shalaga44/missing-annotations-therapist")
        }
      }
    }
  }

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
