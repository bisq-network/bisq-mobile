import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.atomicfu)
}

dependencies {
    androidTestImplementation(libs.androidx.test.compose.junit4)
    androidTestImplementation(libs.androidx.test.compose.manifest)
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.androidx.test.compose.manifest)
}

version = project.findProperty("shared.version") as String

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(project(":shared:domain"))
            implementation(project(":shared:kscan"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.logging.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.navigation.compose)
            implementation(libs.bignum)
            implementation(libs.coil.compose)

            implementation(libs.atomicfu)

            // for parsing urls with io.ktor.http.parseUrl
            implementation(libs.ktor.http.get().toString()) {
                exclude(group = "org.slf4j", module = "slf4j-api") // prevent sl4j exact version problem
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            //implementation(libs.androidx.activity.ktx)
            implementation(libs.koin.android)
        }
        androidUnitTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlin.test.junit)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.test.compose.junit4)
            implementation(libs.androidx.test.junit)
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
    }
}

android {
    namespace = "network.bisq.mobile.shared.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        tasks.matching { task ->
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
    private fun discoverBridgeModules(interopDir: File): List<String> {
        return interopDir.listFiles()?.filter { it.extension == "def" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    /**
     * Get Swift library path without spawning external processes (config cache friendly).
     */
    private fun getSwiftLibPath(): String {
        val developerPath = System.getenv("DEVELOPER_DIR")
            ?: "/Applications/Xcode.app/Contents/Developer"
        // Swift libraries are in the toolchain, not the SDK
        return "$developerPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator"
    }

    private fun getSwiftBridgeOutputDir(): Directory {
        return layout.buildDirectory.dir("swift-bridge").get()
    }

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
        bridgeModules: List<String>
    ) {
        targets.forEach { target ->
            bridgeModules.forEach { moduleName ->
                target.compilations.getByName("main") {
                    cinterops.create(moduleName) {
                        definitionFile.set(project.file("${interopDir.absolutePath}/${moduleName}.def"))
                        includeDirs.allHeaders(interopDir.absolutePath)
                    }
                }
                target.compilations.getByName("test") {
                    cinterops.create(moduleName) {
                        definitionFile.set(project.file("${interopDir.absolutePath}/${moduleName}.def"))
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
        bridgeModules: List<String>
    ) {
        targets.forEach { target ->
            target.binaries.all {
                val objectFiles = bridgeModules.map {
                    getSwiftBridgeOutputDir().file("${it}.o").asFile.absolutePath
                }

                val isMac = System.getProperty("os.name").lowercase().contains("mac")

                if (isMac) {
                    try {
                        val swiftLibPath = getSwiftLibPath()
                        linkerOpts(
                            *objectFiles.toTypedArray(),
                            "-L$swiftLibPath",
                            "-lswiftCore",
                            "-lswiftFoundation",
                            "-lswiftDispatch",
                            "-lswiftObjectiveC",
                            "-lswiftDarwin",
                            "-lswiftCoreFoundation"
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
        val swiftOutputDir = getSwiftBridgeOutputDir()

        val bridgeModules = discoverBridgeModules(interopDir)


        // Detect the current architecture for simulator builds
        val simulatorArch = System.getProperty("os.arch").let { arch ->
            when {
                arch == "aarch64" || arch == "arm64" -> "arm64"
                arch == "x86_64" || arch == "amd64" -> "x86_64"
                else -> "arm64" // default to arm64 for Apple Silicon
            }
        }

        // Create a compile task for each Swift bridge module
        val compileSwiftBridgeTasks = bridgeModules.map { bridgeModuleName ->
            tasks.register<Exec>("compileSwiftBridge_${bridgeModuleName}") {
                group = "build"
                description = "Compile Swift bridge module: $bridgeModuleName for iOS tests"
                notCompatibleWithConfigurationCache("Swift bridge compile Exec is not configuration cache friendly")


                val swiftFile = file("${interopDir}/${bridgeModuleName}.swift")
                val headerFile = file("${interopDir}/${bridgeModuleName}.h")
                val objectFile = swiftOutputDir.file("${bridgeModuleName}.o").asFile

                inputs.files(swiftFile, headerFile)
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
                    swiftOutputDir.asFile.mkdirs()
                    logger.info("Compiling Swift bridge for architecture: $simulatorArch")
                }

                // Compile Swift to object file for simulator with dynamic SDK path
                commandLine(
                    "xcrun",
                    "-sdk", "iphonesimulator",
                    "swiftc",
                    "-emit-object",
                    "-parse-as-library",
                    "-o", objectFile.absolutePath,
                    "-module-name", bridgeModuleName,
                    "-import-objc-header", headerFile.absolutePath,
                    "-target", "${simulatorArch}-apple-ios13.0-simulator",
                    swiftFile.absolutePath
                )

                doLast {
                    logger.info("Successfully compiled $bridgeModuleName Swift bridge for $simulatorArch")
                }
            }
        }

        // Create an aggregate task that compiles all Swift bridges
        val compileSwiftBridge = tasks.register("compileSwiftBridge") {
            group = "build"
            description = "Compile all Swift bridge modules for iOS tests"
            dependsOn(compileSwiftBridgeTasks)
        }

        // Ensure Swift bridge objects are built before linking iOS test binaries
        tasks.matching { it.name.startsWith("link") && it.name.contains("TestIosSimulatorArm64") }.configureEach {
            dependsOn(compileSwiftBridge)
        }
        // Also tie to test Kotlin compilation as a safety net (ensures object files exist by link time)
        tasks.matching { it.name == "compileTestKotlinIosSimulatorArm64" }.configureEach {
            dependsOn(compileSwiftBridge)
        }

        kotlin {
            configureSwiftBridgeCinterops(
                listOf(iosX64(), iosArm64(), iosSimulatorArm64()),
                interopDir,
                bridgeModules
            )

            configureSwiftBridgeLinking(
                listOf(iosSimulatorArm64()),
                bridgeModules,
            )
        }
    }
}

SwiftBridgeConfiguration().configureSwiftBridge()
