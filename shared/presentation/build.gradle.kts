import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

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

// ---- iOS Swift bridge (LocalEncryptionBridge) wiring for tests ----
// Discover bridge modules from .def files in iosClient/iosClient/interop
val interopDir = file("${rootDir.absolutePath}/iosClient/iosClient/interop")
val bridgeModules = interopDir.listFiles()?.filter { it.extension == "def" }?.map { it.nameWithoutExtension } ?: emptyList()

kotlin {
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    val iosSimulatorTargets = listOf(iosSimulatorArm64())

    iosTargets.forEach { target ->
        // Create cinterops for all discovered bridge modules
        bridgeModules.forEach { moduleName ->
            target.compilations.getByName("main") {
                cinterops.create(moduleName) {
                    definitionFile.set(file("${rootDir.absolutePath}/iosClient/iosClient/interop/${moduleName}.def"))
                    includeDirs.allHeaders(rootDir.absolutePath + "/iosClient/iosClient/interop/")
                }
            }
            target.compilations.getByName("test") {
                cinterops.create(moduleName) {
                    definitionFile.set(file("${rootDir.absolutePath}/iosClient/iosClient/interop/${moduleName}.def"))
                    includeDirs.allHeaders(rootDir.absolutePath + "/iosClient/iosClient/interop/")
                }
            }
        }
    }

    iosSimulatorTargets.forEach { target ->
        // Link all Swift bridge implementations for test binaries
        target.binaries.all {
            val objectFiles = bridgeModules.map { layout.buildDirectory.file("swift-bridge/${it}.o").get().asFile.absolutePath }
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
                    logger.warn("Could not determine Swift library path: ${e.message}")
                }
            }
        }
    }
}

// Output directory for Swift bridge object files
val swiftOutputDir = layout.buildDirectory.file("swift-bridge").get().asFile

// Get Swift stdlib path (config-cache friendly)
fun getSwiftLibPath(): String {
    val developerPath = System.getenv("DEVELOPER_DIR")
        ?: "/Applications/Xcode.app/Contents/Developer"
    return "$developerPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator"
}

// Detect simulator architecture
val simulatorArch = System.getProperty("os.arch").let { arch ->
    when {
        arch == "aarch64" || arch == "arm64" -> "arm64"
        arch == "x86_64" || arch == "amd64" -> "x86_64"
        else -> "arm64"
    }
}

// Create a compile task per Swift bridge module
val compileSwiftBridgeTasks = bridgeModules.map { bridgeModuleName ->
    tasks.register<Exec>("compileSwiftBridge_${bridgeModuleName}") {
        group = "build"
        description = "Compile Swift bridge module: $bridgeModuleName for iOS tests"
        notCompatibleWithConfigurationCache("Swift bridge compile Exec is not configuration cache friendly")

        val swiftFile = file("${interopDir}/${bridgeModuleName}.swift")
        val headerFile = file("${interopDir}/${bridgeModuleName}.h")
        val objectFile = file("${swiftOutputDir}/${bridgeModuleName}.o")

        inputs.files(swiftFile, headerFile)
        outputs.file(objectFile)

        onlyIf {
            val isMac = System.getProperty("os.name").lowercase().contains("mac")
            if (!isMac) logger.info("Skipping Swift bridge compilation on non-macOS platform")
            isMac
        }

        doFirst {
            swiftOutputDir.mkdirs()
            logger.info("Compiling Swift bridge for architecture: $simulatorArch")
        }

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

// Aggregate task
val compileSwiftBridge = tasks.register("compileSwiftBridge") {
    group = "build"
    description = "Compile all Swift bridge modules for iOS tests"
    dependsOn(compileSwiftBridgeTasks)
}

// Ensure Swift bridge is built before linking simulator test binaries
tasks.matching { it.name.startsWith("link") && it.name.contains("TestIosSimulatorArm64") }.configureEach {
    dependsOn(compileSwiftBridge)
}
// Safety net: also before compiling test Kotlin for simulator
tasks.matching { it.name == "compileTestKotlinIosSimulatorArm64" }.configureEach {
    dependsOn(compileSwiftBridge)
}
