/*
Copyright 2022 c-fraser

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
import com.diffplug.gradle.spotless.SpotlessExtension
import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt)
  alias(libs.plugins.nexus.publish)
  alias(libs.plugins.jreleaser)
  alias(libs.plugins.dependency.versions)
  `maven-publish`
  signing
}

allprojects {
  group = "io.github.c-fraser"
  version = "0.0.0"

  repositories { mavenCentral() }
}

kotlin {
  jvm()
  js(IR) {
    nodejs()
    browser()
  }
  linuxX64()
  mingwX64()
  macosX64()
  macosArm64()

  sourceSets {
    named("commonTest") {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(libs.kotest.assertions.core)
        implementation(libs.kotest.framework.engine)
      }
    }
  }
}

configure<SpotlessExtension> {
  val ktfmtVersion = "0.39"
  val licenseHeader =
      """
      /*
      Copyright 2022 c-fraser
      
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
    toggleOffOn("@formatter:off", "@formatter:on")
  }

  kotlinGradle {
    ktfmt(ktfmtVersion)
    licenseHeader(licenseHeader, "(import|plugins|rootProject)")
    target(fileTree(rootProject.rootDir) { include("**/*.gradle.kts") })
  }
}

publishing {
  publications.withType<MavenPublication> {
    pom {
      name.set(project.name)
      description.set("${project.name}-${project.version}")
      url.set("https://github.com/c-fraser/graph-it")
      inceptionYear.set("2022")

      issueManagement {
        system.set("GitHub")
        url.set("https://github.com/c-fraser/graph-it/issues")
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
        url.set("https://github.com/c-fraser/graph-it")
        connection.set("scm:git:git://github.com/c-fraser/graph-it.git")
        developerConnection.set("scm:git:ssh://git@github.com/c-fraser/graph-it.git")
      }
    }
  }

  signing {
    publications.withType<MavenPublication>().all mavenPublication@{
      useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_PASSWORD"))
      sign(this@mavenPublication)
    }
  }
}

configure<NexusPublishExtension> {
  repositories {
    sonatype {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
      username.set(System.getenv("SONATYPE_USERNAME"))
      password.set(System.getenv("SONATYPE_PASSWORD"))
    }
  }
}

configure<JReleaserExtension> {
  project {
    website.set("https://github.com/c-fraser/${rootProject.name}")
    authors.set(listOf("c-fraser"))
    license.set("Apache-2.0")
    extraProperties.put("inceptionYear", "2022")
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

tasks {
  withType<KotlinCompile>().all {
    kotlinOptions {
      freeCompilerArgs = listOf()
      jvmTarget = "${JavaVersion.VERSION_11}"
    }
  }

  withType<Jar> { manifest { attributes("Automatic-Module-Name" to "io.github.cfraser.graphit") } }

  val detekt =
      withType<Detekt> {
        jvmTarget = "${JavaVersion.VERSION_11}"
        buildUponDefaultConfig = true
        config.setFrom(rootDir.resolve("detekt.yml"))
      }

  spotlessApply { dependsOn(detekt) }
}
