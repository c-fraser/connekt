plugins {
  `java-library`
  `maven-publish`
  signing
}

dependencies {
  api(project(":connekt-api"))
  implementation(kotlin("test"))

  testImplementation(project(":connekt-test"))
}
