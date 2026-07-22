import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Depend on domain for repository interfaces
            implementation(project(":shared:domain"))

            // Coroutines for StateFlow in mocks and test dispatchers
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)

            // DataStore serializer contract tests
            implementation(libs.androidx.datastore.okio)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            // JUnit annotations for Android test bases
            implementation(libs.junit)

            // Koin for KoinIntegrationTestBase — api so subclasses can use KoinTest
            api(libs.koin.core)
            api(libs.koin.test)
        }
    }
}

android {
    namespace = "network.bisq.mobile.test.utils"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
