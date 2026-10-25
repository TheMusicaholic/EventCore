plugins {
    // Downloads the Java versions the run tasks need when they aren't installed (Java 25 for 26.1+).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "EventCore"
include(":api")
include(":api-test-plugin")
