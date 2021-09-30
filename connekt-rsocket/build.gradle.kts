plugins {
  `java-library`
  id("org.jetbrains.dokka")
  `maven-publish`
  signing
}

dependencies {
  val caffeineCacheVersion: String by rootProject
  val kotlinLoggingVersion: String by rootProject
  val kotlinRetryVersion: String by rootProject
  val kotlinxCoroutinesVersion: String by rootProject
  val rsocketVersion: String by rootProject
  val slf4jVersion: String by rootProject

  api(project(":connekt-api"))
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineCacheVersion")
  implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
  implementation("com.michael-bull.kotlin-retry:kotlin-retry:$kotlinRetryVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
  implementation("io.rsocket:rsocket-core:$rsocketVersion")
  implementation("io.rsocket:rsocket-transport-netty:$rsocketVersion")
  implementation("io.rsocket:rsocket-load-balancer:$rsocketVersion")
  implementation("io.rsocket:rsocket-micrometer:$rsocketVersion")
  testImplementation("io.rsocket:rsocket-transport-local:$rsocketVersion")
  testImplementation("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.test { systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug") }
