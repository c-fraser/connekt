pluginManagement {
  val kotlinVersion: String by settings
  val spotlessVersion: String by settings
  val detektVersion: String by settings
  val nexusPublishVersion: String by settings
  val jreleaserVersion: String by settings
  val versionsVersion: String by settings
  val dokkaVersion: String by settings
  val protobufGradleVersion: String by settings

  plugins {
    kotlin("jvm") version kotlinVersion
    id("com.diffplug.spotless") version spotlessVersion
    id("io.gitlab.arturbosch.detekt") version detektVersion
    id("io.github.gradle-nexus.publish-plugin") version nexusPublishVersion
    id("org.jreleaser") version jreleaserVersion
    id("com.github.ben-manes.versions") version versionsVersion
    id("org.jetbrains.dokka") version dokkaVersion
    id("com.google.protobuf") version protobufGradleVersion
  }

  repositories {
    gradlePluginPortal()
    mavenCentral()
  }

  val atomicfuVersion: String by settings
  val knitVersion: String by settings

  resolutionStrategy {
    eachPlugin {
      when (requested.id.id) {
        "kotlinx-atomicfu" ->
            useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicfuVersion")
        "kotlinx-knit" -> useModule("org.jetbrains.kotlinx:kotlinx-knit:$knitVersion")
      }
    }
  }
}

rootProject.name = "connekt"

include(
    "connekt-api",
    "connekt-jackson",
    "connekt-local",
    "connekt-protobuf",
    "connekt-redis",
    "connekt-rsocket",
    "connekt-test",
    "examples")
