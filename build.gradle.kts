import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import java.net.URL
import java.util.jar.Attributes
import kotlinx.knit.KnitPluginExtension
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

plugins {
  kotlin("jvm") apply false
  id("org.jetbrains.dokka") apply false
  id("com.diffplug.spotless")
  id("io.gitlab.arturbosch.detekt")
  id("io.github.gradle-nexus.publish-plugin")
  id("org.jreleaser")
  id("com.github.ben-manes.versions")
  id("kotlinx-knit")
}

allprojects {
  group = "io.github.c-fraser"
  version = "0.4.0"

  repositories { mavenCentral() }
}

subprojects project@{
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "io.gitlab.arturbosch.detekt")

  plugins.withType<JavaLibraryPlugin>() {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
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

  tasks {
    withType<KotlinCompile>().configureEach {
      kotlinOptions {
        jvmTarget = "${JavaVersion.VERSION_11}"
        freeCompilerArgs =
            listOf(
                "-Xjsr305=strict",
                "-Xjvm-default=all",
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xopt-in=io.github.cfraser.connekt.api.InternalConnektApi",
                "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
      }
    }

    withType<Jar> {
      manifest {
        attributes(
            "${Attributes.Name.IMPLEMENTATION_TITLE}" to this@project.name,
            "${Attributes.Name.IMPLEMENTATION_VERSION}" to this@project.version,
            "Automatic-Module-Name" to
                "io.github.cfraser.connekt.${this@project.name.removePrefix("${rootProject.name}-")}")
      }
    }

    withType<Test> { useJUnitPlatform { excludeTags("integration") } }

    register<Test>("integrationTest") {
      description = "Runs tests annotated with 'integration' tag"

      useJUnitPlatform { includeTags("integration") }
    }

    withType<DokkaTask>().configureEach {
      dokkaSourceSets {
        named("main") {
          moduleName.set(this@project.name)
          runCatching { this@project.file("MODULE.md").takeIf { it.exists() }!! }.onSuccess {
              moduleDocumentation ->
            includes.from(moduleDocumentation)
          }
          platform.set(Platform.jvm)
          jdkVersion.set(JavaVersion.VERSION_11.ordinal)
          sourceLink {
            localDirectory.set(this@project.file("src/main/kotlin"))
            remoteUrl.set(
                URL(
                    "https://github.com/c-fraser/connekt/tree/main/${this@project.name}/src/main/kotlin"))
            remoteLineSuffix.set("#L")
          }
        }
      }
    }

    withType<Detekt> {
      jvmTarget = "${JavaVersion.VERSION_11}"
      buildUponDefaultConfig = true
      config.setFrom(rootDir.resolve("detekt.yml"))
    }
  }

  plugins.withType<MavenPublishPlugin> {
    configure<PublishingExtension> {
      val dokkaJavadocJar by
          tasks.creating(Jar::class) {
            val dokkaJavadoc by tasks.getting(AbstractDokkaTask::class)
            dependsOn(dokkaJavadoc)
            archiveClassifier.set("javadoc")
            from(dokkaJavadoc.outputDirectory.get())
          }

      val sourcesJar by
          tasks.creating(Jar::class) {
            val sourceSets: SourceSetContainer by this@project
            dependsOn(tasks["classes"])
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
          }

      publications {
        create<MavenPublication>("maven") {
          from(this@project.components["java"])
          artifact(dokkaJavadocJar)
          artifact(sourcesJar)
          pom {
            name.set(this@project.name)
            description.set("${this@project.name}-${this@project.version}")
            url.set("https://github.com/c-fraser/connekt")
            inceptionYear.set("2021")

            issueManagement {
              system.set("GitHub")
              url.set("https://github.com/c-fraser/connekt/issues")
            }

            licenses {
              license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
              }
            }

            developers {
              developer {
                id.set("c-fraser")
                name.set("Chris Fraser")
              }
            }

            scm {
              url.set("https://github.com/c-fraser/connekt")
              connection.set("scm:git:git://github.com/c-fraser/connekt.git")
              developerConnection.set("scm:git:ssh://git@github.com/c-fraser/connekt.git")
            }
          }
        }
      }

      plugins.withType<SigningPlugin>() {
        configure<SigningExtension> {
          publications.withType<MavenPublication>().all mavenPublication@{
            useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_PASSWORD"))
            sign(this@mavenPublication)
          }
        }
      }
    }
  }
}

configure<SpotlessExtension> {
  val ktfmtVersion: String by rootProject

  val licenseHeader =
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
      """.trimIndent()

  kotlin {
    ktfmt(ktfmtVersion)
    licenseHeader(licenseHeader)
    target(
        fileTree(rootProject.rootDir) {
          // Exclude the files automatically generated by `kotlinx-knit`
          exclude("examples/src/*/kotlin/io/github/cfraser/connekt/example/knit/*.kt")
          include("**/src/**/*.kt")
        })
  }

  kotlinGradle { ktfmt(ktfmtVersion) }

  java {
    googleJavaFormat()
    licenseHeader(licenseHeader)
    target(fileTree(rootProject.rootDir) { include("**/src/**/*.java") })
  }
}

nexusPublishing {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

jreleaser {
  project {
    website.set("https://github.com/c-fraser/connekt")
    authors.set(listOf("c-fraser"))
    license.set("Apache-2.0")
    extraProperties.put("inceptionYear", "2021")
  }

  release {
    github {
      owner.set("c-fraser")
      overwrite.set(true)
      token.set(System.getenv("GITHUB_TOKEN").orEmpty())
      changelog {
        formatted.set(Active.ALWAYS)
        format.set("- {{commitShortHash}} {{commitTitle}}")
        contributors.enabled.set(false)
        for (status in listOf("added", "changed", "fixed", "removed")) {
          labeler {
            label.set(status)
            title.set(status)
          }
          category {
            title.set(status.capitalize())
            labels.set(listOf(status))
          }
        }
      }
    }
  }
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
            "examples/src/*/kotlin/io/github/cfraser/connekt/example/knit/*.kt")
      }

  spotlessApply { finalizedBy(detektAll) }
}
