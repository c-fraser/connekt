if (JavaVersion.current() < JavaVersion.VERSION_11)
    throw GradleException("Java 11+ is required for this project")

plugins { `kotlin-dsl` }

repositories { mavenCentral() }
