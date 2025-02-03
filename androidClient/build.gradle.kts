import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

version = project.findProperty("client.android.version") as String
val sharedVersion = project.findProperty("shared.version") as String

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AndroidClient"
            isStatic = false
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(compose.runtime)
        }
    }
}

val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

android {
    namespace = "network.bisq.mobile.client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            if (localProperties["KEYSTORE_PATH"] != null) {
                storeFile = file(localProperties["KEYSTORE_PATH"] as String)
                storePassword = localProperties["KEYSTORE_PASSWORD"] as String
                keyAlias = localProperties["CLI_KEY_ALIAS"] as String
                keyPassword = localProperties["CLI_KEY_PASSWORD"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "network.bisq.mobile.client"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/NOTICE.md")
            excludes.add("META-INF/NOTICE.markdown")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "Bisq Connect"
            val version = variant.versionName
            val fileName = "$appName-$version.apk"
            output.outputFileName = fileName
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":shared:presentation"))
    implementation(project(":shared:domain"))
    // FIXME hack to avoid the issue that org.slf4j is not found as we exclude it in shared
    implementation(libs.ktor.client.cio)
    debugImplementation(compose.uiTooling)
}

