import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildconfig)
}

dependencies {
    androidTestImplementation(libs.androidx.test.compose)
    androidTestImplementation(libs.androidx.test.manifest)
}

version = project.findProperty("shared.version") as String

// The following allow us to configure each app type independently and link for example with gradle.properties
// TODO potentially to be refactored into a shared/common module
buildConfig {
    forClass("network.bisq.mobile.client.shared", className = "BuildConfig") {
        buildConfigField("APP_NAME", project.findProperty("client.name").toString())
        buildConfigField("ANDROID_APP_VERSION", project.findProperty("client.android.version").toString())
        buildConfigField("IOS_APP_VERSION", project.findProperty("client.ios.version").toString())
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BUILD_TS", System.currentTimeMillis())
    }
    forClass("network.bisq.mobile.android.node", className = "BuildNodeConfig") {
        buildConfigField("APP_NAME", project.findProperty("node.name").toString())
        buildConfigField("APP_VERSION", project.findProperty("node.android.version").toString())
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BUILD_TS", System.currentTimeMillis())
    }
//    buildConfigField("APP_SECRET", "Z3JhZGxlLWphdmEtYnVpbGRjb25maWctcGx1Z2lu")
//    buildConfigField<String>("OPTIONAL", null)
//    buildConfigField("FEATURE_ENABLED", true)
//    buildConfigField("MAGIC_NUMBERS", intArrayOf(1, 2, 3, 4))
//    buildConfigField("STRING_LIST", arrayOf("a", "b", "c"))
//    buildConfigField("MAP", mapOf("a" to 1, "b" to 2))
//    buildConfigField("FILE", File("aFile"))
//    buildConfigField("URI", uri("https://example.io"))
//    buildConfigField("com.github.gmazzo.buildconfig.demos.kts.SomeData", "DATA", "SomeData(\"a\", 1)")

}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared Presentation Logic, navigation and connection between data and UI"
        homepage = "X"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        podfile = project.file("../../iosClient/Podfile")
        framework {
            baseName = "presentation"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(project(":shared:domain"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.logging.kermit)
            implementation(libs.kotlinx.coroutines)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc02")
            implementation("io.coil-kt.coil3:coil-svg:3.0.0-rc02")

            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")
            //implementation("androidx.navigation:navigation-compose:2.8.3")
            //https://github.com/Kamel-Media/Kamel
            //implementation("media.kamel:kamel-image:1.0.0")
            //implementation("media.kamel:kamel-decoder-svg-std:1.0.0")
            //implementation("io.ktor:ktor-server-netty:3.0.0")
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
//                implementation(kotlin("test"))
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

android {
    namespace = "network.bisq.mobile.shared.presentation"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
    dependencies {
        //implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc02")
        //implementation("io.ktor:ktor-client-android:3.0.0")
    }
//    dependencies {
//        implementation("media.kamel:kamel-fetcher-resources-android:1.0.0")
//    }


//appleMain {
//    dependencies {
//        implementation("io.ktor:ktor-client-darwin:3.0.0")
//    }
//}
//jvmMain {
//    dependencies {
//        implementation("io.ktor:ktor-client-java:3.0.0>")
//    }
//}
