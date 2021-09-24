plugins {
  `java-library`
  id("org.jetbrains.dokka")
  `maven-publish`
  signing
}

dependencies {
  val kotlinxCoroutinesVersion: String by rootProject

  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")
}
