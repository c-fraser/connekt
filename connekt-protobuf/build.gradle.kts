import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  `java-library`
  `maven-publish`
  signing
  idea
  id("com.google.protobuf")
}

val protobufVersion: String by rootProject

dependencies {
  api(project(":connekt-api"))
  implementation("com.google.protobuf:protobuf-java:$protobufVersion")

  testImplementation(project(":connekt-local"))
  testImplementation(project(":connekt-test"))
}

protobuf { protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" } }
