plugins {
  `java-library`
  `maven-publish`
  signing
  id("redis-integration-test")
}

dependencies {
  val lettuceVersion: String by rootProject
  val kotlinxCoroutinesVersion: String by rootProject

  api(project(":connekt-api"))
  api("io.lettuce:lettuce-core:$lettuceVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")

  testImplementation(project(":connekt-test"))
}
