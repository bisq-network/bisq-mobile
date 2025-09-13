import com.google.protobuf.gradle.proto
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.protobuf)
}

version = project.findProperty("node.android.version") as String
val versionCodeValue = (project.findProperty("node.android.version.code") as String).toInt()
val sharedVersion = project.findProperty("shared.version") as String

kotlin {
    // using JDK21 for full bisq2 compatibility
    jvmToolchain(21)
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        val androidMain by getting {
            androidMain.dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
            }
            androidUnitTest.dependencies {
                implementation(libs.mockk)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
                implementation(libs.robolectric)
            }
            androidInstrumentedTest.dependencies {
                implementation(libs.mockk)
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)

                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.junit)
            }
            kotlin.srcDirs("src/androidMain/kotlin")
        }
    }
}

val localProperties = Properties()
localProperties.load(File(rootDir, "local.properties").inputStream())

android {
    namespace = "network.bisq.mobile.node"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            if (localProperties["KEYSTORE_PATH"] != null) {
                storeFile = file(localProperties["KEYSTORE_PATH"] as String)
                storePassword = localProperties["KEYSTORE_PASSWORD"] as String
                keyAlias = localProperties["KEY_ALIAS"] as String
                keyPassword = localProperties["KEY_PASSWORD"] as String
            }
        }
    }

    sourceSets {
        getByName("debug") {
            java {
                srcDir("src/main/resources")
                // Debug build only includes debug proto sources
                srcDir("${layout.buildDirectory}/generated/source/proto/debug/java")
            }
            proto {
                srcDir("${layout.buildDirectory}/extracted-include-protos/debug")
            }
        }
        getByName("release") {
            java {
                srcDir("src/release/resources")
                // Release build only includes release proto sources
                srcDir("${layout.buildDirectory}/generated/source/proto/release/java")
            }
            proto {
                srcDir("${layout.buildDirectory}/extracted-include-protos/release")
            }
        }
    }

    defaultConfig {
        applicationId = "network.bisq.mobile.node"
        minSdk = libs.versions.android.node.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        multiDexEnabled = true
        versionCode = versionCodeValue
        versionName = project.version.toString()
        buildConfigField("String", "APP_VERSION", "\"${version}\"")
        buildConfigField("String", "SHARED_VERSION", "\"${sharedVersion}\"")

        // Memory management configuration
        // Default: standard heap. Release builds use large heap to handle heavy network sync
        // while debug builds use standard heap to catch memory leaks early in development
        manifestPlaceholders["largeHeap"] = "false"

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
            // the following exclude are needeD to avoid protobuf hanging build when merging release resources for java
            // Exclude the conflicting META-INF files
            excludes.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE*.md")
            excludes.add("META-INF/NOTICE*.md")
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/NOTICE.markdown")
            pickFirsts.add("**/protobuf/**/*.class")
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

            // Keep standard heap in debug for leak detection
            manifestPlaceholders["largeHeap"] = "false"

            // Disable minification in debug to avoid lock verification issues
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = project.name
            val version = variant.versionName
            val fileName = "$appName-$version.apk"
            output.outputFileName = fileName
        }
    }
    buildFeatures {
        buildConfig = true
    }

    // ABI splits disabled to prevent packaging conflicts with kmp-tor native libraries
    compileOptions {
        // for bisq2 jars full compatibility
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    // needed for aab files renaming
    setProperty("archivesBaseName", getArtifactName(defaultConfig))
}

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

// Compatible with macOS on Apple Silicon
val archSuffix = if (Os.isFamily(Os.FAMILY_MAC)) ":osx-x86_64" else ""

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protob.get()}$archSuffix"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.inputs.dir("${layout.buildDirectory.get()}/extracted-include-protos/debug")
            task.builtins {
                create("java")
            }
        }
    }
}
dependencies {
    implementation(project(":shared:presentation"))
    implementation(project(":shared:domain"))
    debugImplementation(compose.uiTooling)

    // bisq2 core dependencies
    implementation(libs.androidx.multidex)
    implementation(libs.google.guava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.typesafe.config)

    implementation(libs.bouncycastle)
    implementation(libs.bouncycastle.pg)

    implementation(libs.bisq.core.common)
    implementation(libs.bisq.core.i18n)
    implementation(libs.bisq.core.persistence)
    implementation(libs.bisq.core.security)
    // # bisq:core:network#
    implementation(libs.bisq.core.network.network)
    implementation(libs.bisq.core.network.network.identity)
    implementation(libs.bisq.core.network.socks5.socket.channel)
    implementation(libs.bisq.core.network.i2p) //todo should not be needed
    implementation(libs.chimp.jsocks)
    implementation(libs.failsafe)
    implementation(libs.apache.httpcomponents.httpclient)
    // ##### network ######
    implementation(libs.bisq.core.identity)
    implementation(libs.bisq.core.account)
    implementation(libs.bisq.core.settings)
    implementation(libs.bisq.core.bonded.roles)
    implementation(libs.bisq.core.user)
    implementation(libs.bisq.core.contract)
    implementation(libs.bisq.core.offer)
    implementation(libs.bisq.core.trade)
    implementation(libs.bisq.core.support)
    implementation(libs.bisq.core.application)
    implementation(libs.bisq.core.chat)
    implementation(libs.bisq.core.presentation)
    implementation(libs.bisq.core.bisq.easy)

    // protobuf
    implementation(libs.protoc)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.logging.kermit)

    // kmp-tor for embedded Tor support
    implementation(libs.kmp.tor.runtime)
    implementation(libs.kmp.tor.resource.exec)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.splashscreen)
}

// ensure tests run on the same Java version as the main code
tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21)) // Update from 17 to 21
        }
    )
}

fun getArtifactName(defaultConfig: com.android.build.gradle.internal.dsl.DefaultConfig): String {
    return "Bisq-${defaultConfig.versionName}_${defaultConfig.versionCode}"
}

// Configure ProGuard mapping file management using shared script
extra["moduleName"] = "androidNode"
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")