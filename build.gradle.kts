import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.0"
    id("com.android.library") version "8.7.2"
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    explicitApi()
    jvmToolchain(19)

    jvm()
    js {
        browser()
        nodejs()
    }
    // WASM and similar
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }
    // Android
    androidTarget {
        publishLibraryVariants("release")
    }
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    // Linux
    linuxArm64()
    linuxX64()
    // macOS
    macosArm64()
    macosX64()
    // MingW
    mingwX64()
    // iOS
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    // tvOS
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    // watchOS
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {}
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = group.toString()
    compileSdk = 34
    defaultConfig {
        minSdk = 30
    }
}

tasks.register<Copy>("setupLocalRepo") {
    group = "build"
    description = "Setup local repository with all dependencies"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDir = file("${projectDir}/local-repo")
    val relativePath = relativePath(destinationDir.parent)

    kotlin.sourceSets.forEach {
        from(it.kotlin.sourceDirectories) {
            into(relativePath)
        }
    }

    // Copy Maven dependencies
    val mavenUserHome = file("${System.getProperty("user.home")}/.m2")
    val mavenRepoDir = file("$mavenUserHome/repository")
    from(mavenRepoDir) {
        include("**/*")
    }
    into(destinationDir)

    // Copy Gradle plugins
    val gradleUserHome = gradle.gradleUserHomeDir
    val gradlePluginsDir = file("$gradleUserHome/caches/modules-2/files-2.1")
    from(gradlePluginsDir) {
        include("**/*")
    }
    into(destinationDir)

    // Copy Kotlin/Native (konan) dependencies
    val konanUserHome = file("${System.getProperty("user.home")}/.konan")
    val konanDir = file("$konanUserHome/cache")
    from(konanDir) {
        include("**/*")
    }
    into(destinationDir)

    // Counterhack fixing atrocities commited by IllumiNATO
    doLast {
        val rootDir = file("${projectDir}/local-repo")
        fun traverse(file: File, level: Int) {
            if (file.isDirectory) {
                println("Directory: ${file.path}")

                file.listFiles()?.forEach {
                    if(it.isFile && it.exists() && level == 4){
                        val path = it.path.split('/').toMutableList()
                        path.removeAt(path.lastIndex-1)
                        val newDir = file("/" + path.joinToString("/") + "/")
                        if(!newDir.exists()) {
                            println("Moving file up: $newDir")
                            it.copyTo(newDir)
                            it.delete()
                        } else {
                            it.delete()
                        }
                    }
                    traverse(it, level + 1)
                }

                if(file.listFiles()?.isEmpty() == true) {
                    println("Deleting empty directory: ${file.path}")
                    file.delete()
                }
            } else {
                println("File: ${file.path}")
            }
        }
        traverse(rootDir, 0)
    }
}

tasks.register<Zip>("createSourcePackage") {
    group = "distribution"
    description = "Package the project for source distribution"
    dependsOn("setupLocalRepo")
    from(".") {
        include(
            "local-repo/**",
            "src/**", "build.gradle.kts", "settings.gradle.kts",
            "gradlew", "gradlew.bat", "gradle/**",
            "LICENSE", "README.md", ".gitignore",
            ".gradle/**", ".kotlin/**")
    }
    archiveFileName.set("shitty-random-${version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions").get().asFile)
}