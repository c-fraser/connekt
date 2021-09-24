plugins {
  `java-library`
  id("org.jetbrains.dokka")
  `maven-publish`
  signing
}

dependencies {
  val caffeineCacheVersion: String by rootProject
  val kotlinxCoroutinesVersion: String by rootProject
  val rsocketVersion: String by rootProject

  api(project(":connekt-api"))
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineCacheVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
  implementation("io.rsocket:rsocket-core:$rsocketVersion")
  implementation("io.rsocket:rsocket-transport-netty:$rsocketVersion")
  implementation("io.rsocket:rsocket-micrometer:$rsocketVersion")
}
