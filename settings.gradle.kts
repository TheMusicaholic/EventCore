plugins {
    // Downloads the Java toolchain (Java 25 for Minecraft 26.x) if it isn't installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "EventCore"
include(":api")
include(":api-test-plugin")
