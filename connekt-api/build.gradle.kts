plugins {
  `java-library`
  `maven-publish`
  signing
  id("kotlinx-atomicfu")
}

atomicfu {
  val atomicfuVersion: String by rootProject

  dependenciesVersion = atomicfuVersion
  transformJvm = true
  variant = "VH"
  verbose = false
}

dependencies {
  val kotlinxCoroutinesVersion: String by rootProject
  val kotlinLoggingVersion: String by rootProject

  api("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion")
  api("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxCoroutinesVersion")

  testImplementation(project(":connekt-local"))
  testImplementation(project(":connekt-test"))
}
