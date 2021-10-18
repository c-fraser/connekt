plugins {
  `java-library`
  `maven-publish`
  signing
}

dependencies {
  val jacksonVersion: String by rootProject

  api(project(":connekt-api"))
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

  testImplementation(project(":connekt-local"))
  testImplementation(project(":connekt-test"))
}
