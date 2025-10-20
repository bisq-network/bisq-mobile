import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

version = project.findProperty("client.android.version") as String
val versionCodeValue = (project.findProperty("client.android.version.code") as String).toInt()
val sharedVersion = project.findProperty("shared.version") as String
val appName = project.findProperty("client.name") as String

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
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(project(":shared:presentation"))
            implementation(project(":shared:domain"))

            implementation(libs.logging.kermit)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp) // for sl4j dependency issue

            implementation(libs.androidx.core.splashscreen)
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
        versionCode = versionCodeValue
        versionName = version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // the following excludes are needed to avoid protobuf hanging build when merging release resources for java
            // Exclude the conflicting META-INF files
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE*.md")
            excludes.add("META-INF/NOTICE*.md")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/NOTICE.markdown")
            pickFirsts += listOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/services/**",
                "META-INF/*.version"
            )
        }
        jniLibs {
            // for apk release builds after tor inclusion
            // If multiple .so files exist across dependencies, pick the first and avoid conflicts
            pickFirsts += listOf(
                "lib/**/libtor.so",
                "lib/**/libcrypto.so",
                "lib/**/libevent*.so",
                "lib/**/libssl.so",
                "lib/**/libsqlite*.so",
                // Data store
                "lib/**/libdatastore_shared_counter.so",
            )
            // Exclude problematic native libraries
            excludes += listOf(
                "**/libmagtsync.so",
                "**/libMEOW*.so"
            )
            // Required for kmp-tor exec resources - helps prevent EOCD corruption
            useLegacyPackaging = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
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
            val version = variant.versionName
            val fileName = "${appName.replace(" ", "_")}-$version.apk"
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
    // needed for aab files renaming
    setProperty("archivesBaseName", getArtifactName(defaultConfig))
}

dependencies {
    debugImplementation(compose.uiTooling)
}

fun getArtifactName(defaultConfig: com.android.build.gradle.internal.dsl.DefaultConfig): String {
//    val date = SimpleDateFormat("yyyyMMdd").format(Date())
    return "${appName.replace(" ", "")}-${defaultConfig.versionName}_${defaultConfig.versionCode}"
}

// Configure ProGuard mapping file management using shared script
extra["moduleName"] = "androidClient"
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")

// Ensure generateResourceBundles runs before Android build tasks
afterEvaluate {
    val generateResourceBundlesTask = project(":shared:domain").tasks.findByName("generateResourceBundles")
    if (generateResourceBundlesTask != null) {
        tasks.matching { task ->
            task.name.startsWith("compile") ||
            task.name.startsWith("assemble") ||
            task.name.startsWith("bundle") ||
            task.name.contains("Build")
        }.configureEach {
            dependsOn(generateResourceBundlesTask)
        }
    }
}