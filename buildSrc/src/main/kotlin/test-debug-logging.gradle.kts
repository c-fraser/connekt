plugins.withType<JavaLibraryPlugin> {
  dependencies {
    val slf4jVersion: String by rootProject

    "implementation"("org.slf4j:slf4j-simple:$slf4jVersion")
  }

  tasks.withType<Test> { systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug") }
}
