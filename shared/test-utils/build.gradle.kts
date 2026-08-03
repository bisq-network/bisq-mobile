import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    // Compose is required for the Android presentation test bases
    // (BisqComposeTestSupport and friends declare @Composable lambdas).
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

// Compose code lives only in androidMain (test.presentation.*); iOS source sets have
// no Compose runtime, so keep the Compose compiler off the native targets.
composeCompiler {
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
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

            // Presentation test bases (test.presentation.*) — api because base classes
            // expose presentation types (NavigationManager, GlobalUiManager) to subclasses.
            // One-way dependency: test-utils androidMain -> presentation main. Presentation's
            // androidUnitTest depends back on this module's main — acyclic at source-set level.
            api(project(":shared:presentation"))

            // Compose runtime + rule for the Compose UI test bases; api so subclasses see
            // ComposeContentTestRule via the exposed composeTestRule property.
            implementation(libs.compose.runtime)
            api(libs.androidx.test.compose.junit4)
            implementation(libs.androidx.test.junit)

            // NoopNavigationManager implements NavigationManager (NavHostController params)
            implementation(libs.navigation.compose)

            // Presentation test bases create relaxed mocks and static mocks
            implementation(libs.mockk)
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
