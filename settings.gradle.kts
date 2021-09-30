pluginManagement {
  val kotlinVersion: String by settings
  val spotlessVersion: String by settings
  val detektVersion: String by settings
  val nexusPublishVersion: String by settings
  val versionsVersion: String by settings
  val dokkaVersion: String by settings
  val knitVersion: String by settings

  plugins {
    kotlin("jvm") version kotlinVersion
    id("com.diffplug.spotless") version spotlessVersion
    id("io.gitlab.arturbosch.detekt") version detektVersion
    id("io.github.gradle-nexus.publish-plugin") version nexusPublishVersion
    id("com.github.ben-manes.versions") version versionsVersion
    id("org.jetbrains.dokka") version dokkaVersion
  }

  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  resolutionStrategy {
    eachPlugin {
      when (requested.id.id) {
        "kotlinx-knit" -> useModule("org.jetbrains.kotlinx:kotlinx-knit:$knitVersion")
      }
    }
  }
}

rootProject.name = "connekt"

include("connekt-api", "connekt-rsocket", "examples")
