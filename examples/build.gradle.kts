import io.gitlab.arturbosch.detekt.Detekt

plugins { id("redis-integration-test") }

dependencies {
  val knitVersion: String by rootProject
  val slf4jVersion: String by rootProject

  implementation(project(":connekt-redis"))
  implementation(project(":connekt-rsocket"))
  testImplementation("org.jetbrains.kotlinx:kotlinx-knit-test:$knitVersion")
  testImplementation("org.slf4j:slf4j-nop:$slf4jVersion")
}

configurations.implementation { exclude(group = "org.slf4j", module = "slf4j-simple") }

tasks.withType<Detekt> { enabled = false }
