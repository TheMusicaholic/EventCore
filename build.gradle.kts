plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

group = "me.david"
version = "2.3"
description = "Event Server System with tons of useful commands and features"

java {
    toolchain {
        // Java 21 runs 1.21.11, 26.1+ needs Java 25: built for 21, the plugin runs on both.
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") // PaperMC
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://maven.canvasmc.io/snapshots") // CanvasMC
}

dependencies {
    implementation(project(":api"))
    compileOnly(libs.canvas.api)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnlyApi(libs.placeholderapi)
}

publishing {
    repositories {
        maven {
            name = "allay"
            url = uri("https://repo.allay-studios.com/public")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["shadow"])
        }
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
    }

    assemble {
        dependsOn(shadowJar)
    }

    // The same jar runs on every version from 1.21.11 to 26.3, so the server can be any of them:
    // ./gradlew runServer -PminecraftVersion=26.3 (1.21.11 if not given)
    val version = providers.gradleProperty("minecraftVersion").getOrElse("1.21.11")

    // 1.8.8 - 1.16.5   = Java 8
    // 1.17             = Java 16
    // 1.18 - 1.20.4    = Java 17
    // 1.20.5 - 1.21.11 = Java 21
    // 26.1+            = Java 25
    val javaVersion = JavaLanguageVersion.of(if (version.startsWith("1.")) 21 else 25)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.12.0/ViaVersion-5.12.0.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.12.0/ViaBackwards-5.12.0.jar")
        // EventCore depends on it. By id, the Fabric builds have the same version numbers: 7.4.2 is the last one
        // for 1.21.11 that runs on Java 21, 7.4.6 runs on 26.1 to 26.3 (Java 25).
        modrinth("worldedit", if (version.startsWith("1.")) "p8T2aZ8U" else "YFnqiUlL")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
        }

        jvmArgs = jvmArgsExternal
    }

    runPaper.folia.registerTask {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/folia/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
        }

        jvmArgs = jvmArgsExternal
    }
}
