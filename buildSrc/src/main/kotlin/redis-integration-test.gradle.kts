import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins { id("com.bmuschko.docker-remote-api") }

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

  named<Test>("integrationTest") {
    dependsOn(startRedisContainer)
    finalizedBy(stopRedisContainer)
    systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
  }
}
