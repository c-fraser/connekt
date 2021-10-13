import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins {
  `java-library`
  `maven-publish`
  signing
  id("com.bmuschko.docker-remote-api")
}

dependencies {
  val lettuceVersion: String by rootProject
  val kotlinxCoroutinesVersion: String by rootProject

  api(project(":connekt-api"))
  api("io.lettuce:lettuce-core:$lettuceVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")

  testImplementation(project(":connekt-test"))
}

tasks {
  val pullRedisImage by creating(DockerPullImage::class) { image.set("redis:latest") }

  val createRedisContainer by
      creating(DockerCreateContainer::class) {
        dependsOn(pullRedisImage)
        targetImageId("redis:latest")
        hostConfig.portBindings.set(listOf("6379:6379"))
      }

  val startRedisContainer by
      creating(DockerStartContainer::class) {
        dependsOn(createRedisContainer)
        targetContainerId(createRedisContainer.containerId)
      }

  val stopRedisContainer by
      creating(DockerStopContainer::class) { targetContainerId(createRedisContainer.containerId) }

  integrationTest {
    dependsOn(startRedisContainer)
    finalizedBy(stopRedisContainer)
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
  }
}
