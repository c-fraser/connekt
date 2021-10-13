plugins {
  `java-library`
  `maven-publish`
  signing
}

dependencies {
  val slf4jVersion: String by rootProject

  api(project(":connekt-api"))
  implementation(kotlin("test"))
  implementation("org.slf4j:slf4j-simple:$slf4jVersion")
}
