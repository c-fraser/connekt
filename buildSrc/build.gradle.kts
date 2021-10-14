if (JavaVersion.current() < JavaVersion.VERSION_11)
    throw GradleException("Java 11+ is required for this project")

plugins { `kotlin-dsl` }

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies { implementation("com.bmuschko:gradle-docker-plugin:7.1.0") }
