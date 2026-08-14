import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.kover)
    // Sentry-KMP plugin must be applied to every module whose iOS test/main
    // binary transitively links Sentry-KMP. This module transitively pulls
    // Sentry-KMP through its `implementation(project(":shared:domain"))`
    // dependency, so its K/N test framework needs Sentry.framework on the
    // link path — without the plugin coordinating with cocoapods at this
    // module level the link fails with `ld: framework 'Sentry' not found`.
    alias(libs.plugins.sentry.kotlin.multiplatform)
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
}

version = project.findProperty("shared.version") as String

// -------------------- Module References --------------------
val sharedTestUtilsModule = ":shared:test-utils"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    iosArm64()
    iosSimulatorArm64()

    // The Sentry Cocoa SDK pod is declared here (matching apps/clientApp and
    // shared/domain) so the K/N linker for this module's iOS test framework
    // finds Sentry.framework. Sentry-KMP's cinterop bindings reference Sentry
    // Cocoa symbols at link time; without this declaration
    // `linkDebugTestIosSimulatorArm64` fails with `ld: framework 'Sentry' not
    // found`. Shares the iosClient/Podfile with the host app, so a single
    // `pod install` covers every module.
    cocoapods {
        summary = "Bisq Mobile — shared presentation module"
        homepage = "https://github.com/bisq-network/bisq-mobile"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        podfile = project.file("../../iosClient/Podfile")
        pod("Sentry") {
            // Version and `-fmodules` MUST match apps/clientApp's declaration
            // (Sentry-KMP 0.26.0 → Sentry Cocoa 8.58.2; -fmodules avoids the
            // `SentryMechanismMeta declared twice` cinterop crash).
            version = "8.58.2"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // put your multiplatform dependencies here
            implementation(project(":shared:domain"))
            implementation(project(":shared:kscan"))

            api(libs.compose.material3)
            api(libs.compose.components.resources)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material.icons.extended)

            // AndroidX
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)

            // KotlinX
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(libs.koin.compose)
            implementation(libs.koin.core)

            // Other libraries
            implementation(libs.atomicfu)
            implementation(libs.bignum)
            implementation(libs.coil.compose)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.logging.kermit)
            implementation(libs.navigation.compose)
        }

        androidMain.dependencies {
            // AndroidX
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)

            // Koin
            implementation(libs.koin.android)
        }

        androidUnitTest.dependencies {
            // AndroidX
            implementation(libs.androidx.test.compose.junit4)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.compose.manifest)

            // Kotlin
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)

            // KotlinX
            implementation(libs.kotlinx.coroutines.test)

            // Other libraries
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.robolectric)

            // Test utilities
            implementation(project(sharedTestUtilsModule))
        }

        val commonTest by getting {
            dependencies {
                // Kotlin
                implementation(libs.kotlin.test)

                // Compose
                implementation(libs.compose.ui.test)

                // Test utilities
                implementation(project(sharedTestUtilsModule))
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

android {
    namespace = "network.bisq.mobile.shared.presentation"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Enable resources for Robolectric unit tests
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// Ensure generateResourceBundles runs before compilation
afterEvaluate {
    val generateResourceBundlesTask = project(":shared:domain").tasks.findByName("generateResourceBundles")
    if (generateResourceBundlesTask != null) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            dependsOn(generateResourceBundlesTask)
        }
        tasks
            .matching { task ->
                task.name.contains("compile", ignoreCase = true) ||
                    task.name.contains("build", ignoreCase = true)
            }.configureEach {
                dependsOn(generateResourceBundlesTask)
            }
    }
}

/**
 * Helper class to setup Swift bridge interops
 */
class SwiftBridgeConfiguration {
    /**
     * Discover all bridge modules in the specified interop directory.
     *
     * @param interopDir The directory containing Swift bridge .def files
     * @return List of bridge module names
     */
    private fun discoverBridgeModules(interopDir: File): List<String> =
        interopDir
            .listFiles()
            ?.filter { it.extension == "def" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()

    /**
     * Get Swift library path without spawning external processes (config cache friendly).
     */
    private fun getSwiftLibPath(sdkName: String): String {
        val developerPath =
            System.getenv("DEVELOPER_DIR")
                ?: "/Applications/Xcode.app/Contents/Developer"
        // Swift libraries are in the toolchain, not the SDK
        return "$developerPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$sdkName"
    }

    // Namespaced per SDK: device and simulator objects are not interchangeable, and a shared
    // output path lets a stale artifact from one SDK get linked into the other's binary.
    private fun getSwiftBridgeOutputDir(sdkName: String): Directory = layout.buildDirectory.dir("swift-bridge/$sdkName").get()

    private fun sdkNameFor(target: KotlinNativeTarget): String = if (target.name == "iosArm64") "iphoneos" else "iphonesimulator"

    /**
     * Configure cinterops for all discovered bridge modules. this is required for discovering the bridge in both test and main.
     * But "main" part is sufficient for running on devices.
     *
     * @param targets The iOS native targets to configure
     * @param interopDir The directory containing Swift bridge files
     * @param bridgeModules List of bridge module names to configure
     */
    private fun configureSwiftBridgeCinterops(
        targets: List<KotlinNativeTarget>,
        interopDir: File,
        bridgeModules: List<String>,
    ) {
        targets.forEach { target ->
            bridgeModules.forEach { moduleName ->
                target.compilations.getByName("main") {
                    cinterops.create(moduleName) {
                        definitionFile.set(project.file("${interopDir.absolutePath}/$moduleName.def"))
                        includeDirs.allHeaders(interopDir.absolutePath)
                    }
                }
                target.compilations.getByName("test") {
                    cinterops.create(moduleName) {
                        definitionFile.set(project.file("${interopDir.absolutePath}/$moduleName.def"))
                        includeDirs.allHeaders(interopDir.absolutePath)
                    }
                }
            }
        }
    }

    /**
     * Configure Swift bridge linking for given iOS targets. This is required for running iOS tests using bridge modules.
     *
     * @param targets The iOS native targets to configure
     * @param bridgeModules List of bridge module names to link
     */
    private fun configureSwiftBridgeLinking(
        targets: List<KotlinNativeTarget>,
        bridgeModules: List<String>,
    ) {
        targets.forEach { target ->
            val sdkName = sdkNameFor(target)
            target.binaries.all {
                val objectFiles =
                    bridgeModules.map {
                        getSwiftBridgeOutputDir(sdkName).file("$it.o").asFile.absolutePath
                    }

                val isMac = System.getProperty("os.name").lowercase().contains("mac")

                if (isMac) {
                    try {
                        val swiftLibPath = getSwiftLibPath(sdkName)
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
                        project.logger.warn("Could not determine Swift library path: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Running this is required to configure swift bridges properly for modules
     */
    fun configureSwiftBridge() {
        val interopDir = file("${rootDir.absolutePath}/iosClient/iosClient/interop")

        val bridgeModules = discoverBridgeModules(interopDir)

        // Detect the current architecture for simulator builds
        val simulatorArch =
            System.getProperty("os.arch").let { arch ->
                when {
                    arch == "aarch64" || arch == "arm64" -> "arm64"
                    arch == "x86_64" || arch == "amd64" -> "x86_64"
                    else -> "arm64" // default to arm64 for Apple Silicon
                }
            }

        // Device objects are always arm64; the simulator arch follows the host.
        val targetTripleBySdk =
            mapOf(
                "iphonesimulator" to "$simulatorArch-apple-ios16.0-simulator",
                "iphoneos" to "arm64-apple-ios16.0",
            )

        // Create a compile task per Swift bridge module PER SDK: device and simulator objects are
        // not interchangeable (`ld: building for 'iOS', but linking in object file built for
        // 'iOS-simulator'`), so each variant gets its own task and output dir.
        val compileTasksBySdk =
            targetTripleBySdk.mapValues { (sdkName, targetTriple) ->
                bridgeModules.map { bridgeModuleName ->
                    tasks.register<Exec>("compileSwiftBridge_${bridgeModuleName}_$sdkName") {
                        group = "build"
                        description = "Compile Swift bridge module $bridgeModuleName for $sdkName"
                        notCompatibleWithConfigurationCache("Swift bridge compile Exec is not configuration cache friendly")

                        val swiftFile = file("$interopDir/$bridgeModuleName.swift")
                        val headerFile = file("$interopDir/$bridgeModuleName.h")
                        val objectFile = getSwiftBridgeOutputDir(sdkName).file("$bridgeModuleName.o").asFile

                        inputs.files(swiftFile, headerFile)
                        // Exec does not track commandLine: declare these so an SDK/triple change
                        // invalidates the task instead of leaving a stale object file.
                        inputs.property("sdkName", sdkName)
                        inputs.property("targetTriple", targetTriple)
                        outputs.file(objectFile)

                        // Only run on macOS
                        onlyIf {
                            val isMac = System.getProperty("os.name").lowercase().contains("mac")
                            if (!isMac) {
                                logger.info("Skipping Swift bridge compilation on non-macOS platform")
                            }
                            isMac
                        }

                        doFirst {
                            objectFile.parentFile.mkdirs()
                            logger.info("Compiling Swift bridge $bridgeModuleName for $targetTriple")
                        }

                        commandLine(
                            "xcrun",
                            "-sdk",
                            sdkName,
                            "swiftc",
                            "-emit-object",
                            "-parse-as-library",
                            "-o",
                            objectFile.absolutePath,
                            "-module-name",
                            bridgeModuleName,
                            "-import-objc-header",
                            headerFile.absolutePath,
                            "-target",
                            targetTriple,
                            swiftFile.absolutePath,
                        )

                        doLast {
                            logger.info("Successfully compiled $bridgeModuleName Swift bridge for $targetTriple")
                        }
                    }
                }
            }

        val compileSwiftBridgeSimulator =
            tasks.register("compileSwiftBridgeIphonesimulator") {
                group = "build"
                description = "Compile all Swift bridge modules for the iOS simulator"
                dependsOn(compileTasksBySdk.getValue("iphonesimulator"))
            }
        val compileSwiftBridgeDevice =
            tasks.register("compileSwiftBridgeIphoneos") {
                group = "build"
                description = "Compile all Swift bridge modules for iOS devices"
                dependsOn(compileTasksBySdk.getValue("iphoneos"))
            }
        // Umbrella kept for external dependents (e.g. clientApp's link tasks depend on it by name).
        val compileSwiftBridge =
            tasks.register("compileSwiftBridge") {
                group = "build"
                description = "Compile all Swift bridge modules for all iOS SDKs"
                dependsOn(compileSwiftBridgeSimulator, compileSwiftBridgeDevice)
            }

        // Ensure Swift bridge objects are built before linking iOS test binaries
        tasks.matching { it.name.startsWith("link") && it.name.contains("TestIosSimulatorArm64") }.configureEach {
            dependsOn(compileSwiftBridgeSimulator)
        }
        // Also ensure Swift bridge objects are built before linking iOS main binaries (for frameworks)
        tasks.matching { it.name.startsWith("link") && it.name.contains("IosSimulatorArm64") && !it.name.contains("Test") }.configureEach {
            dependsOn(compileSwiftBridgeSimulator)
        }
        tasks.matching { it.name.startsWith("link") && it.name.contains("IosArm64") && !it.name.contains("Test") }.configureEach {
            dependsOn(compileSwiftBridgeDevice)
        }
        // Also tie to test Kotlin compilation as a safety net (ensures object files exist by link time)
        tasks.matching { it.name == "compileTestKotlinIosSimulatorArm64" }.configureEach {
            dependsOn(compileSwiftBridgeSimulator)
        }

        kotlin {
            configureSwiftBridgeCinterops(
                listOf(iosArm64(), iosSimulatorArm64()),
                interopDir,
                bridgeModules,
            )

            configureSwiftBridgeLinking(
                listOf(iosArm64(), iosSimulatorArm64()),
                bridgeModules,
            )
        }
    }
}

SwiftBridgeConfiguration().configureSwiftBridge()
