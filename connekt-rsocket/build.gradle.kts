plugins {
  `java-library`
  `maven-publish`
  signing
}

dependencies {
  val micrometerVersion: String by rootProject
  val caffeineCacheVersion: String by rootProject
  val kotlinRetryVersion: String by rootProject
  val kotlinxCoroutinesVersion: String by rootProject
  val rsocketVersion: String by rootProject

  api(project(":connekt-api"))
  api("io.micrometer:micrometer-core:$micrometerVersion")
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineCacheVersion")
  implementation("com.michael-bull.kotlin-retry:kotlin-retry:$kotlinRetryVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
  implementation("io.rsocket:rsocket-core:$rsocketVersion")
  implementation("io.rsocket:rsocket-transport-netty:$rsocketVersion")
  implementation("io.rsocket:rsocket-load-balancer:$rsocketVersion")
  implementation("io.rsocket:rsocket-micrometer:$rsocketVersion")

  testImplementation(project(":connekt-test"))
  testImplementation("io.rsocket:rsocket-transport-local:$rsocketVersion")
}

tasks.test { systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug") }
