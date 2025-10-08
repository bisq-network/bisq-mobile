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
            // FIXME hack to avoid the issue that org.slf4j is not found as we exclude it in shared
            implementation(libs.ktor.client.cio)

            implementation(libs.logging.kermit)

            implementation(libs.kmp.tor.runtime)
            implementation(libs.kmp.tor.resource.exec)
            implementation(libs.ktor.client.okhttp)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)

            implementation(libs.androidx.core.splashscreen)

            implementation(libs.kmp.tor.runtime)
            implementation(libs.kmp.tor.resource.exec)
            implementation(libs.ktor.client.okhttp)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.kmp.tor.resource.noexec)
        }
    }
}

val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

android {
    namespace = "network.bisq.mobile.client"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    // pin ndk version for deterministic builds
    ndkVersion = libs.versions.android.ndk.get()

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
        multiDexEnabled = true
        versionCode = versionCodeValue
        versionName = project.version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")

        // Memory management configuration
        // Default: extended heap. Turn false to test for mem leaks reducing heap size.
        manifestPlaceholders["largeHeap"] = "true"

        // for apk release build after tor inclusion
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    // Disable ABI splits to avoid packaging conflicts with kmp-tor
    splits {
        abi {
            isEnable = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
            // General full shrinking brings issues with protobuf in jars
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
            isDebuggable = false
            isCrunchPngs = true

            manifestPlaceholders["largeHeap"] = "true"
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Reduce GC logging noise in debug builds
            buildConfigField("String", "GC_LOG_LEVEL", "\"WARN\"")

            // Turn false to use standard heap in debug for leak detection
            manifestPlaceholders["largeHeap"] = "true"

            // Disable minification in debug to avoid lock verification issues
            isMinifyEnabled = false
            isShrinkResources = false
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

    // ABI splits disabled to prevent packaging conflicts with kmp-tor native libraries
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

dependencies {
    implementation(project(":shared:presentation"))
    implementation(project(":shared:domain"))
    debugImplementation(compose.uiTooling)

    implementation(libs.androidx.multidex)

    implementation(libs.koin.android)
    implementation(libs.logging.kermit)

    // kmp-tor for embedded Tor support
    implementation(libs.kmp.tor.runtime)
    implementation(libs.kmp.tor.resource.exec)
    implementation(libs.ktor.client.okhttp)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.splashscreen)

    implementation(libs.process.phoenix)
}