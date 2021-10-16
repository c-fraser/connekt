plugins {
  `java-library`
  `maven-publish`
  signing
}

dependencies {
  val jacksonVersion: String by rootProject

  api(project(":connekt-api"))
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  testImplementation(project(":connekt-test"))
}

tasks.test { systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug") }
