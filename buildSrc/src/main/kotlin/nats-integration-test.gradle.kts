import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins { id("com.bmuschko.docker-remote-api") }

tasks {
  val pullNatsImage by creating(DockerPullImage::class) { image.set("nats:latest") }

  val createNatsContainer by
      creating(DockerCreateContainer::class) {
        dependsOn(pullNatsImage)
        targetImageId("nats:latest")
        hostConfig.portBindings.set(listOf("4222:4222"))
      }

  val startNatsContainer by
      creating(DockerStartContainer::class) {
        dependsOn(createNatsContainer)
        targetContainerId(createNatsContainer.containerId)
      }

  val stopNatsContainer by
      creating(DockerStopContainer::class) { targetContainerId(createNatsContainer.containerId) }

  named<Test>("integrationTest") {
    dependsOn(startNatsContainer)
    finalizedBy(stopNatsContainer)
  }
}
