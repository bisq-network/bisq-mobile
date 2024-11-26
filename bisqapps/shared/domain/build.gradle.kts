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
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.jetbrains.serialization.gradle.plugin)

            implementation(libs.multiplatform.settings)

            configurations.all {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
//            implementation("com.russhwolf:multiplatform-settings-datastore:1.2.0")
//
//            implementation("androidx.test:core:1.5.0")
//            implementation("androidx.test.ext:junit:1.1.5")
//            implementation("androidx.test.espresso:espresso-core:3.5.1")
//            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
