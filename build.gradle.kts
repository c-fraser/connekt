import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.knit.KnitPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") apply false
  id("com.diffplug.spotless")
  id("io.gitlab.arturbosch.detekt")
  id("io.github.gradle-nexus.publish-plugin")
  id("com.github.ben-manes.versions")
  id("kotlinx-knit")
}

allprojects {
  group = "io.github.c-fraser"
  version = "0.0.0"

  repositories { mavenCentral() }
}

subprojects {
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "io.gitlab.arturbosch.detekt")

  plugins.withId("java-library") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }

  tasks {
    withType<KotlinCompile>().configureEach {
      kotlinOptions {
        jvmTarget = "${JavaVersion.VERSION_11}"
        freeCompilerArgs =
            listOf(
                "-Xjsr305=strict",
                "-Xjvm-default=all",
                "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
      }
    }

    withType<Test> { useJUnitPlatform { excludeTags("integration") } }

    create("integrationTest", Test::class) {
      description = "Runs tests annotated with 'integration' tag"

      useJUnitPlatform { includeTags("integration") }
    }
  }

  dependencies {
    val junitVersion: String by rootProject

    "implementation"(kotlin("stdlib"))
    "testImplementation"(kotlin("test"))
    "testImplementation"(kotlin("test-junit5"))
    "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
  }

  tasks.withType<Detekt> {
    jvmTarget = "${JavaVersion.VERSION_11}"
    buildUponDefaultConfig = true
    config.setFrom(rootDir.resolve("detekt.yml"))
  }
}

configure<SpotlessExtension> {
  val ktfmtVersion: String by rootProject

  kotlin {
    ktfmt(ktfmtVersion)
    licenseHeader(
        """
        /*
        Copyright 2021 c-fraser
  
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at
  
            https://www.apache.org/licenses/LICENSE-2.0
  
        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
        */
        """.trimIndent())
    target(
        fileTree(rootProject.rootDir) {
          // Exclude the files automatically generated by `kotlinx-knit`
          exclude("examples/src/*/kotlin/io/github/cfraser/connekt/example/*.kt")
          include("**/src/**/*.kt")
        })
  }

  kotlinGradle { ktfmt(ktfmtVersion) }
}

configure<KnitPluginExtension> {
  siteRoot = "https://github.com/c-fraser/connekt"
  files = fileTree(rootProject.rootDir) { include("README.md") }
  rootDir = rootProject.rootDir
}

tasks {
  val detektAll by
      registering(Detekt::class) {
        jvmTarget = "${JavaVersion.VERSION_11}"
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootDir.resolve("detekt.yml"))
        setSource(files(projectDir))
        include("**/*.kt", "**/*.kts")
        exclude(
            "**/build/**",
            "**/resources/**",
            // Exclude the files automatically generated by `kotlinx-knit`
            "examples/src/*/kotlin/io/github/cfraser/connekt/example/*.kt")
      }

  spotlessApply { finalizedBy(detektAll) }
}
