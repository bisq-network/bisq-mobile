import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

// -------------------- Version Configuration --------------------
version = project.findProperty("client.android.version") as String
val versionCodeValue = (project.findProperty("client.android.version.code") as String).toInt()
val sharedVersion = project.findProperty("shared.version") as String
val appName = project.findProperty("client.name") as String
val iosVersion = project.findProperty("client.ios.version") as String

// -------------------- Module References --------------------
val clientFrameworkBaseName = "ClientApp"
val clientAppModuleName = "clientApp"
val sharedPresentationModule = ":shared:presentation"
val sharedDomainModule = ":shared:domain"
val sharedKScanModule = ":shared:kscan"

dependencies {
    debugImplementation(compose.uiTooling)
    // IMPORTANT: This is needed for Robolectric tests to work in both variants
    // The fact that its needed to be included in the build might be a compose issue
    // We accept that for now as the lib size is minimal and won't impact the prod code.
    implementation(libs.androidx.test.compose.manifest)
}

// -------------------- Kotlin Multiplatform Configuration --------------------
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    fun org.jetbrains.kotlin.gradle.plugin.mpp.Framework.configureSharedExports() {
        export(project(sharedPresentationModule))
        export(project(sharedDomainModule))
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = clientFrameworkBaseName
            configureSharedExports()

            // Link Swift bridge object files (only from domain module to avoid duplicates)
            val swiftBridgeModules = listOf("LocalEncryptionBridge")
            val domainSwiftBridgeDir =
                project(":shared:domain")
                    .layout.buildDirectory
                    .dir("swift-bridge")
                    .get()
                    .asFile

            val objectFiles =
                swiftBridgeModules.map { moduleName ->
                    File(domainSwiftBridgeDir, "$moduleName.o").absolutePath
                }

            val isMac = System.getProperty("os.name").lowercase().contains("mac")
            if (isMac) {
                try {
                    val swiftLibPath = "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator"
                    linkerOpts(
                        *objectFiles.toTypedArray(),
                        "-L$swiftLibPath",
                        "-lswiftCore",
                        "-lswiftFoundation",
                        "-lswiftDispatch",
                        "-lswiftObjectiveC",
                        "-lswiftDarwin",
                        "-lswiftCoreFoundation",
                    )
                } catch (e: Exception) {
                    project.logger.warn("Could not configure Swift library path: ${e.message}")
                }
            }
        }
    }

    cocoapods {
        summary = "Bisq Connect Application"
        homepage = "X"
        version = iosVersion
        ios.deploymentTarget = "16.0"
        podfile = project.file("../../iosClient/Podfile")
        framework {
            baseName = clientFrameworkBaseName
            isStatic = true
            configureSharedExports()
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Project modules
            api(project(sharedDomainModule))
            api(project(sharedKScanModule))
            api(project(sharedPresentationModule))

            // Compose
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            // AndroidX
            implementation(libs.androidx.datastore.okio)

            // KotlinX
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(libs.koin.compose)
            implementation(libs.koin.core)

            // Ktor
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.network)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Other libraries
            implementation(libs.atomicfu)
            implementation(libs.bignum)
            implementation(libs.logging.kermit)
            implementation(libs.navigation.compose)
        }

        commonTest.dependencies {
            // Kotlin
            implementation(libs.kotlin.test)

            // KotlinX
            implementation(libs.kotlinx.coroutines.test)

            // Koin
            implementation(libs.koin.test)
        }

        androidMain.dependencies {
            // Compose
            implementation(compose.preview)

            // AndroidX
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)

            // Koin
            implementation(libs.koin.android)

            // Ktor
            implementation(libs.ktor.client.okhttp)
        }

        androidUnitTest.dependencies {
            // AndroidX
            implementation(libs.androidx.test.compose.junit4)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.test.junit)

            // Kotlin
            implementation(libs.kotlin.test.junit)

            // KotlinX
            implementation(libs.kotlinx.coroutines.test)

            // Other libraries
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.robolectric)
        }
    }
}

// -------------------- Local Properties --------------------
val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

// -------------------- Android Configuration --------------------
android {
    namespace = "network.bisq.mobile.client"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

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
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = versionCodeValue
        versionName = version.toString()

        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")
    }

    // Enable resources for Robolectric unit tests
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // The following excludes are needed to avoid protobuf hanging build when merging release resources for java
            // Exclude the conflicting META-INF files
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE*.md")
            excludes.add("META-INF/NOTICE*.md")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/NOTICE.markdown")

            pickFirsts +=
                listOf(
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/services/**",
                    "META-INF/*.version",
                )
        }
        jniLibs {
            // For apk release builds after tor inclusion
            // If multiple .so files exist across dependencies, pick the first and avoid conflicts
            pickFirsts +=
                listOf(
                    "lib/**/libtor.so",
                    "lib/**/libcrypto.so",
                    "lib/**/libevent*.so",
                    "lib/**/libssl.so",
                    "lib/**/libsqlite*.so",
                    "lib/**/libdatastore_shared_counter.so",
                )
            // Exclude problematic native libraries
            excludes +=
                listOf(
                    "**/libmagtsync.so",
                    "**/libMEOW*.so",
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
                "proguard-rules.pro",
            )
            dependenciesInfo {
                includeInApk = false
                includeInBundle = false
            }
            isDebuggable = false
            isCrunchPngs = true
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

    // Needed for aab files renaming
    setProperty("archivesBaseName", getArtifactName(defaultConfig))
}

// -------------------- Dependencies --------------------
dependencies {
    debugImplementation(compose.uiTooling)
}

// -------------------- Build Tasks Configuration --------------------
// Ensure generateResourceBundles runs before Android build tasks
afterEvaluate {
    val generateResourceBundlesTask = project(sharedDomainModule).tasks.findByName("generateResourceBundles")
    if (generateResourceBundlesTask != null) {
        tasks
            .matching { task ->
                task.name.startsWith("compile") ||
                    task.name.startsWith("assemble") ||
                    task.name.startsWith("bundle") ||
                    task.name.contains("Build")
            }.configureEach {
                dependsOn(generateResourceBundlesTask)
            }
    }
}

// -------------------- Helper Functions --------------------
fun getArtifactName(defaultConfig: com.android.build.gradle.internal.dsl.DefaultConfig): String = "${appName.replace(" ", "")}-${defaultConfig.versionName}_${defaultConfig.versionCode}"

// -------------------- Swift Bridge Configuration --------------------
// Ensure Swift bridge objects are built before linking iOS frameworks
tasks
    .matching {
        it.name.startsWith("link") &&
            (it.name.contains("IosSimulatorArm64") || it.name.contains("IosArm64")) &&
            !it.name.contains("Test")
    }.configureEach {
        dependsOn(":shared:domain:compileSwiftBridge")
        dependsOn(":shared:presentation:compileSwiftBridge")
    }

// -------------------- ProGuard Mapping Configuration --------------------
extra["moduleName"] = clientAppModuleName
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")
