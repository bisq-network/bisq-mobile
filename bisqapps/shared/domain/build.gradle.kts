plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
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

            // Coroutines
            // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

            // Ktor client for HTTP requests
            implementation("io.ktor:ktor-client-core:3.0.0")
            implementation("io.ktor:ktor-client-cio:3.0.0") // Replace with the appropriate engine for your platform
            implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
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
