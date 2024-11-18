plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version "2.0.21"
}

version = project.findProperty("shared.version") as String

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared Domain business logic and KOJOs"
        homepage = "X"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        podfile = project.file("../../iosClient/Podfile")
        framework {
            baseName = "domain"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.logging.kermit)

            implementation("io.ktor:ktor-client-core:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("io.ktor:ktor-client-serialization:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("io.ktor:ktor-client-json:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("io.ktor:ktor-client-cio:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("io.ktor:ktor-client-content-negotiation:3.0.1") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("org.jetbrains.kotlin.plugin.serialization:org.jetbrains.kotlin.plugin.serialization.gradle.plugin:2.0.21") {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
            implementation("com.squareup.okio:okio:3.9.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        iosMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}

android {
    namespace = "network.bisq.mobile.shared.domain"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
