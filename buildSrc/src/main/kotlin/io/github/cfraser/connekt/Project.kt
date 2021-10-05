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
package io.github.cfraser.connekt

import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

/** The GitHub repository identifier for the project. */
const val githubRepository = "c-fraser/connekt"

/** The GitHub URL for the project. */
const val projectUrl = "https://github.com/$githubRepository"

/** Create the signed publication for the Java library in the [Project]. */
fun Project.configurePublishing() {
  check(plugins.hasPlugin(JavaLibraryPlugin::class))
  check(plugins.hasPlugin(MavenPublishPlugin::class))
  check(plugins.hasPlugin(SigningPlugin::class))

  val sourcesJar by tasks.named<Jar>("kotlinSourcesJar")
  val javadocJar by tasks.named<Jar>("dokkaJavadocJar")

  extensions.configure<PublishingExtension>("publishing") {
    publications {
      create("maven", MavenPublication::class) {
        from(components["java"])
        artifact(sourcesJar)
        artifact(javadocJar)
        pom {
          name.set(name)
          description.set("$name-$version")
          url.set(projectUrl)
          inceptionYear.set("2021")

          issueManagement {
            system.set("GitHub")
            url.set("$projectUrl/issues")
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
            url.set(projectUrl)
            connection.set("scm:git:git://github.com/$githubRepository.git")
            developerConnection.set("scm:git:ssh://git@github.com/$githubRepository.git")
          }
        }
      }
    }

    publications.withType(MavenPublication::class).all mavenPublication@{
      extensions.configure<SigningExtension>("signing") {
        useInMemoryPgpKeys(System.getenv("GPG_SIGNING_KEY"), System.getenv("GPG_PASSWORD"))
        sign(this@mavenPublication)
      }
    }
  }
}
