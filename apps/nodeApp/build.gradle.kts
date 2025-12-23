import com.google.protobuf.gradle.proto
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.protobuf)
}

// -------------------- Version Configuration --------------------
version = project.findProperty("node.android.version") as String
val versionCodeValue = (project.findProperty("node.android.version.code") as String).toInt()
val sharedVersion = project.findProperty("shared.version") as String
val appName = project.findProperty("node.name") as String

// -------------------- Module References --------------------
val sharedPresentationModule = ":shared:presentation"
val sharedDomainModule = ":shared:domain"
val nodeAppModuleName = "nodeApp"

// -------------------- Kotlin Multiplatform Configuration --------------------
kotlin {
    // using JDK21 for full bisq2 compatibility
    jvmToolchain(21)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Project modules
            api(project(sharedPresentationModule))

            // Compose
            implementation(compose.foundation)

            // Other libraries
            implementation(libs.navigation.compose)
        }

        androidMain.dependencies {
            // Compose
            implementation(compose.preview)

            // AndroidX
            implementation(libs.androidx.activity.compose)
        }

        androidUnitTest.dependencies {
            // Kotlin
            implementation(libs.kotlin.test.junit)

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
    namespace = "network.bisq.mobile.node"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    // pin ndk version for deterministic builds
    ndkVersion = libs.versions.android.ndk.get()

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
                srcDir(layout.buildDirectory.dir("/generated/source/proto/debug/java"))
            }
            proto {
                srcDir(layout.buildDirectory.dir("/extracted-include-protos/debug"))
            }
        }
        getByName("release") {
            java {
                srcDir("src/release/resources")
                // Release build only includes release proto sources
                srcDir(layout.buildDirectory.dir("/generated/source/proto/release/java"))
            }
            proto {
                srcDir(layout.buildDirectory.dir("/extracted-include-protos/release"))
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
        // Default: extended heap. Turn false to test for mem leaks reducing heap size.
        manifestPlaceholders["largeHeap"] = "true"

        // ABI filters for APK release build after Tor inclusion
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
            // Exclude conflicting META-INF files to avoid protobuf build issues
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
            // Pick first for duplicate native libraries across dependencies
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
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Reduce GC logging noise in debug builds
            buildConfigField("String", "GC_LOG_LEVEL", "\"WARN\"")

            // Disable minification in debug to avoid lock verification issues
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("profile") {
            initWith(getByName("release"))
            // Make debuggable so Android Studio can attach allocation tracking on all devices
            isDebuggable = true
            // Easier symbol readability in profiler; flip to true to mimic release exactly
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".profile"
            versionNameSuffix = "-profile"
            matchingFallbacks += listOf("release")
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
        // For bisq2 jars full compatibility
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Needed for aab files renaming
    setProperty("archivesBaseName", getArtifactName(defaultConfig))
}

// -------------------- Protobuf Configuration --------------------
// Compatible with macOS on Apple Silicon
val archSuffix = if (Os.isFamily(Os.FAMILY_MAC)) ":osx-x86_64" else ""

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protob.get()}$archSuffix"
    }
    generateProtoTasks {
        all().forEach { task ->
            val variantName = Regex("(debug|release|profile)", RegexOption.IGNORE_CASE)
                .find(task.name)?.value?.lowercase() ?: "debug"
            task.inputs.dir(layout.buildDirectory.dir("/extracted-include-protos/$variantName"))
            task.builtins {
                create("java")
            }
        }
    }
}

// -------------------- Dependencies --------------------
dependencies {
    // Project modules
    implementation(project(sharedPresentationModule))
    implementation(project(sharedDomainModule))

    // Debug tools
    debugImplementation(compose.uiTooling)

    // Android libraries
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.splashscreen)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Bisq2 core dependencies
    implementation(libs.google.guava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.typesafe.config)

    implementation(libs.bouncycastle)
    implementation(libs.bouncycastle.pg)

    // Bisq2 core modules
    implementation(libs.bisq.core.common)
    implementation(libs.bisq.core.i18n)
    implementation(libs.bisq.core.persistence)
    implementation(libs.bisq.core.security)
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

    // Bisq2 network modules
    implementation(libs.bisq.core.network.network)
    implementation(libs.bisq.core.network.network.identity)
    implementation(libs.bisq.core.network.socks5.socket.channel)
    implementation(libs.bisq.core.network.i2p)
    implementation(libs.chimp.jsocks)
    implementation(libs.failsafe)
    implementation(libs.apache.httpcomponents.httpclient)

    // Protobuf
    implementation(libs.protoc)

    // Dependency injection & logging
    implementation(libs.koin.android)
    implementation(libs.logging.kermit)
}

// -------------------- Build Tasks Configuration --------------------
// Ensure tests run on the same Java version as the main code
tasks.withType<Test> {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    )
}

// Ensure generateResourceBundles runs before Android build tasks
afterEvaluate {
    val generateResourceBundlesTask =
        project(sharedDomainModule).tasks.findByName("generateResourceBundles")
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
extra["moduleName"] = nodeAppModuleName
apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")
