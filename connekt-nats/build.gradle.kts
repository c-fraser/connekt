plugins {
  `java-library`
  `maven-publish`
  signing
  id("nats-integration-test")
}

dependencies {
  val natsJavaVersion: String by rootProject

  api(project(":connekt-api"))
  implementation("io.nats:jnats:$natsJavaVersion")

  testImplementation(project(":connekt-test"))
}
