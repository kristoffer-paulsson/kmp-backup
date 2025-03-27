/*plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}*/

pluginManagement {
    repositories {
        flatDir {
            dirs("file://${rootDir}/local-repo")
        }
        maven {
            url = uri("file://${rootDir}/local-repo")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmp-backup"

