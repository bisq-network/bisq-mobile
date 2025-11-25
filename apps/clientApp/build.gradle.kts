import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
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

dependencies {
    debugImplementation(compose.uiTooling)
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
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = clientFrameworkBaseName
            configureSharedExports()
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
            api(project(sharedPresentationModule))
            api(project(sharedDomainModule))
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.components.resources)

            implementation(libs.navigation.compose)
            implementation(libs.logging.kermit)
            implementation(libs.androidx.datastore.okio)
            implementation(libs.atomicfu)
            implementation(libs.bignum)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.network)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp) // For slf4j dependency issue
            implementation(libs.androidx.core.splashscreen)
        }

        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.mockk)
        }
    }
}

// -------------------- Local Properties --------------------
val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

// -------------------- Android Configuration --------------------
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
            // The following excludes are needed to avoid protobuf hanging build when merging release resources for java
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
            // For apk release builds after tor inclusion
            // If multiple .so files exist across dependencies, pick the first and avoid conflicts
            pickFirsts += listOf(
                "lib/**/libtor.so",
                "lib/**/libcrypto.so",
                "lib/**/libevent*.so",
                "lib/**/libssl.so",
                "lib/**/libsqlite*.so",
                "lib/**/libdatastore_shared_counter.so"
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

// -------------------- Helper Functions --------------------
fun getArtifactName(defaultConfig: com.android.build.gradle.internal.dsl.DefaultConfig): String {
    return "${appName.replace(" ", "")}-${defaultConfig.versionName}_${defaultConfig.versionCode}"
}

// -------------------- ProGuard Mapping Configuration --------------------
extra["moduleName"] = clientAppModuleName
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")