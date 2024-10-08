import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  kotlin("kapt")
//  id("org.jetbrains.dokka")

  signing
  `maven-publish`
  id("org.jmailen.kotlinter")
}

dependencies {
  

  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

  kapt("com.google.auto.service:auto-service:1.0.1")
  compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

  testImplementation(kotlin("test-junit5"))
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
  testImplementation(enforcedPlatform("org.junit:junit-bom:5.9.1"))
}

tasks.withType<KotlinCompile> {
  kotlinOptions.freeCompilerArgs += listOf(
    "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
  )
}

tasks.withType<Test> {
  enabled = false
  useJUnitPlatform()
}

tasks.register("sourcesJar", Jar::class) {
  group = "build"
  description = "Assembles Kotlin sources"

  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
  dependsOn(tasks.classes)
}

/*tasks.register("dokkaJar", Jar::class) {
  group = "documentation"
  description = "Assembles Kotlin docs with Dokka"

  archiveClassifier.set("javadoc")
  from(tasks.dokkaHtml)
  dependsOn(tasks.dokkaHtml)
}*/
/*afterEvaluate {
  tasks["dokkaHtml"].dependsOn(tasks.getByName("kaptKotlin"))
}*/
signing {
  setRequired(provider { gradle.taskGraph.hasTask("publish") })
  sign(publishing.publications)
}

publishing {
  publications {
    create<MavenPublication>("default") {
      from(components["java"])
      artifact(tasks["sourcesJar"])
//      artifact(tasks["dokkaJar"])

      pom {
        name.set(project.name)
        description.set("Kotlin Compiler Plugin that automatically adds missing annotations to your codebase based on configurable criteria")
        url.set("https://github.com/bnorm/missing-annotations-therapist")

        licenses {
          license {
            name.set("Apache License 2.0")
            url.set("https://github.com/bnorm/missing-annotations-therapist/blob/master/LICENSE.txt")
          }
        }
        scm {
          url.set("https://github.com/bnorm/missing-annotations-therapist")
          connection.set("scm:git:git://github.com/bnorm/missing-annotations-therapist.git")
        }
        developers {
          developer {
            name.set("Shalaga")
            url.set("https://github.com/shalaga")
          }
        }
      }
    }
  }

  repositories {
    if (
      hasProperty("sonatypeUsername") &&
      hasProperty("sonatypePassword") &&
      hasProperty("sonatypeSnapshotUrl") &&
      hasProperty("sonatypeReleaseUrl")
    ) {
      maven {
        val sonatypeUrlProperty = when {
          version.toString().endsWith("-SNAPSHOT") -> "sonatypeSnapshotUrl"
          else -> "sonatypeReleaseUrl"
        }
        setUrl(property(sonatypeUrlProperty) as String)
        credentials {
          username = property("sonatypeUsername") as String
          password = property("sonatypePassword") as String
        }
      }
    }
    maven {
      name = "test"
      url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
    }
  }
}
