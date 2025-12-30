import com.android.build.gradle.tasks.factory.AndroidUnitTest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.InputStreamReader
import java.util.Properties
import kotlin.io.path.Path

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.atomicfu)
}

version = project.findProperty("shared.version") as String

val bisqCoreVersion: String by extra {
    findTomlVersion("bisq-core")
}
val bisqApiVersion: String by extra {
    findTomlVersion("bisq-api")
}

// NOTE: The following allow us to configure each app type independently and link for example with gradle.properties
// local.properties overrides any property if you need to setup for example local networking
// TODO potentially to be refactored into a shared/common module
buildConfig {
    useKotlinOutput { internalVisibility = false }
    forClass("network.bisq.mobile.client.shared", className = "BuildConfig") {
        buildConfigField("APP_NAME", project.findProperty("client.name").toString())
        buildConfigField(
            "ANDROID_APP_VERSION",
            project.findProperty("client.android.version").toString(),
        )
        buildConfigField("IOS_APP_VERSION", project.findProperty("client.ios.version").toString())
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BISQ_API_VERSION", bisqApiVersion)
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        // networking setup
        buildConfigField("WS_PORT", project.findProperty("client.x.trustednode.port").toString())
        buildConfigField("WS_ANDROID_HOST", project.findProperty("client.android.trustednode.ip").toString())
        buildConfigField("WS_IOS_HOST", project.findProperty("client.ios.trustednode.ip").toString())
        buildConfigField(
            "IS_DEBUG",
            (
                project.findProperty("bisq.isDebug")?.toString()?.toBoolean()
                    ?: project.gradle.startParameter.taskNames
                        .any { it.contains("debug", ignoreCase = true) }
            ),
        )
    }
    forClass("network.bisq.mobile.android.node", className = "BuildNodeConfig") {
        buildConfigField("APP_NAME", project.findProperty("node.name").toString())
        buildConfigField("APP_VERSION", project.findProperty("node.android.version").toString())
        buildConfigField("TRADE_PROTOCOL_VERSION", "1.0")
        buildConfigField("TRADE_OFFER_VERSION", 1)
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        buildConfigField("BISQ_CORE_VERSION", bisqCoreVersion)
        // Note: Update when updating kmp-tor lib
        buildConfigField("TOR_VERSION", "0.4.8.17") // is TOR DAEMON version
        buildConfigField(
            "IS_DEBUG",
            (
                project.findProperty("bisq.isDebug")?.toString()?.toBoolean()
                    ?: project.gradle.startParameter.taskNames
                        .any { it.contains("debug", ignoreCase = true) }
            ),
        )
    }
//    buildConfigField("APP_SECRET", "Z3JhZGxlLWphdmEtYnVpbGRjb25maWctcGx1Z2lu")
//    buildConfigField<String>("OPTIONAL", null)
//    buildConfigField("FEATURE_ENABLED", true)
//    buildConfigField("MAGIC_NUMBERS", intArrayOf(1, 2, 3, 4))
//    buildConfigField("STRING_LIST", arrayOf("a", "b", "c"))
//    buildConfigField("MAP", mapOf("a" to 1, "b" to 2))
//    buildConfigField("FILE", File("aFile"))
//    buildConfigField("URI", uri("https://example.io"))
//    buildConfigField("com.github.gmazzo.buildconfig.demos.kts.SomeData", "DATA", "SomeData(\"a\", 1)")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kmp.tor.resource.exec)
            implementation(libs.process.phoenix)
        }
        commonMain.dependencies {
            // AndroidX
            implementation(libs.androidx.datastore.okio)

            // KotlinX
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // Koin
            implementation(libs.koin.core)

            // Ktor - network only, used by KmpTorService for TCP socket verification
            implementation(libs.ktor.network)

            // Other libraries
            implementation(libs.atomicfu)
            implementation(libs.bignum)
            implementation(libs.kmp.tor.runtime)
            implementation(libs.logging.kermit)
            api(libs.okio) // api to allow platform specific path conversion for kmp-tor

            configurations.all {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
        }

        commonTest.dependencies {
            // Kotlin
            implementation(libs.kotlin.test)

            // KotlinX
            implementation(libs.kotlinx.coroutines.test)

            // Koin
            implementation(libs.koin.test)
        }

        androidUnitTest.dependencies {
            // Kotlin
            implementation(libs.kotlin.test.junit)

            // Other libraries
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.robolectric)
        }

        androidInstrumentedTest.dependencies {
            // AndroidX
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.espresso.core)
            implementation(libs.androidx.test.junit)

            // Kotlin
            implementation(libs.kotlin.test.junit)

            // KotlinX
            implementation(libs.kotlinx.coroutines.test)

            // Other libraries
            implementation(libs.junit)
        }

        iosMain.dependencies {
            // Koin
            implementation(libs.koin.core)

            // Ktor
            implementation(libs.ktor.client.darwin)

            // Other libraries
            implementation(libs.kmp.tor.resource.noexec)
        }

        iosTest.dependencies {
            // add ios specifics
        }
    }
}

android {
    namespace = "network.bisq.mobile.shared.domain"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // packaging is added for LocalEncryptionInstrumentedTest, which needs this
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
            pickFirsts +=
                listOf(
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/services/**",
                    "META-INF/*.version",
                )
        }
        jniLibs {
            // Pick first for duplicate native libraries across dependencies
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
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Create a task class to ensure proper serialization for configuration cache compatibility
abstract class GenerateResourceBundlesTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val resourceDir = inputDir.asFile.get()

        val bundleNames: List<String> =
            listOf(
                "default",
                "application",
                "authorized_role",
                "bisq_easy",
                "reputation",
                "chat",
                "support",
                "user",
                "account",
                "network",
                "settings",
                "payment_method",
                "mobile", // custom for mobile client
            )

        val languageCodes = listOf("en", "af_ZA", "cs", "de", "es", "it", "pcm", "pt_BR", "ru")

        val bundlesByCode: Map<String, List<ResourceBundle>> =
            languageCodes.associateWith { languageCode ->
                bundleNames.mapNotNull { bundleName ->
                    val code = if (languageCode.lowercase() == "en") "" else "_$languageCode"
                    val fileName = "$bundleName$code.properties"
                    var file = Path(resourceDir.path, fileName).toFile()

                    if (!file.exists()) {
                        // Fall back to English default properties if no translation file
                        file = Path(resourceDir.path, "$bundleName.properties").toFile()
                        if (!file.exists()) {
                            logger.warn("File not found: ${file.absolutePath}")
                            return@mapNotNull null // Skip missing files
                        }
                    }

                    val properties = Properties()

                    // Use InputStreamReader to ensure UTF-8 encoding
                    file.inputStream().use { inputStream ->
                        InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                            properties.load(reader)
                        }
                    }
                    val rawMap = properties.entries.associate { it.key.toString() to it.value.toString() }
                    ResourceBundle(rawMap, bundleName, languageCode)
                }
            }

        bundlesByCode.forEach { (languageCode, bundles) ->
            val outputFile: File = outputDir.get().file("GeneratedResourceBundles_$languageCode.kt").asFile
            val generatedCode =
                StringBuilder().apply {
                    appendLine("package network.bisq.mobile.i18n")
                    appendLine()
                    appendLine("// Auto-generated file. Do not modify manually.")
                    appendLine("object GeneratedResourceBundles_$languageCode {")
                    appendLine("    val bundles = mapOf(")
                    bundles.forEach { bundle ->
                        appendLine("        \"${bundle.bundleName}\" to mapOf(")
                        bundle.map.forEach { (key, value) ->
                            val escapedValue =
                                value
                                    .replace("\\", "\\\\") // Escape backslashes
                                    .replace("\"", "\\\"") // Escape double quotes
                                    .replace("\n", "\\n") // Escape newlines
                            appendLine("            \"$key\" to \"$escapedValue\",")
                        }
                        appendLine("        ),")
                    }
                    appendLine("    )")
                    appendLine("}")
                }

            outputFile.parentFile.mkdirs()
            outputFile.writeText(generatedCode.toString(), Charsets.UTF_8)
        }
    }

    data class ResourceBundle(
        val map: Map<String, String>,
        val bundleName: String,
        val languageCode: String,
    )
}

tasks.register<GenerateResourceBundlesTask>("generateResourceBundles") {
    group = "build"
    description = "Generate a Kotlin file with hardcoded ResourceBundle data"
    inputDir.set(layout.projectDirectory.dir("src/commonMain/resources/mobile"))
    // Using build dir still not working on iOS
    // Thus we use the source dir as target
    outputDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/network/bisq/mobile/i18n"))
}

// Make all compile tasks depend on generateResourceBundles
tasks.withType<KotlinCompile>().configureEach {
    dependsOn("generateResourceBundles")
}

// For Android compilation tasks
tasks.withType<AndroidUnitTest>().configureEach {
    dependsOn("generateResourceBundles")
}

// For general compilation tasks
tasks.matching { it.name.contains("compile", ignoreCase = true) }.configureEach {
    dependsOn("generateResourceBundles")
}

fun findTomlVersion(versionName: String): String {
    val tomlFile = file("../../gradle/libs.versions.toml")
    val tomlContent = tomlFile.readText()
    val versionRegex = Regex("$versionName\\s*=\\s*\"([^\"]+)\"")
    val matchResult = versionRegex.find(tomlContent)
    return matchResult?.groups?.get(1)?.value ?: "unknown"
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
    private fun getSwiftLibPath(): String {
        val developerPath =
            System.getenv("DEVELOPER_DIR")
                ?: "/Applications/Xcode.app/Contents/Developer"
        // Swift libraries are in the toolchain, not the SDK
        return "$developerPath/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator"
    }

    private fun getSwiftBridgeOutputDir(): Directory = layout.buildDirectory.dir("swift-bridge").get()

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
            target.binaries.all {
                val objectFiles =
                    bridgeModules.map {
                        getSwiftBridgeOutputDir().file("$it.o").asFile.absolutePath
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
        val swiftOutputDir = getSwiftBridgeOutputDir()

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

        // Create a compile task for each Swift bridge module
        val compileSwiftBridgeTasks =
            bridgeModules.map { bridgeModuleName ->
                tasks.register<Exec>("compileSwiftBridge_$bridgeModuleName") {
                    group = "build"
                    description = "Compile Swift bridge module: $bridgeModuleName for iOS tests"
                    notCompatibleWithConfigurationCache("Swift bridge compile Exec is not configuration cache friendly")

                    val swiftFile = file("$interopDir/$bridgeModuleName.swift")
                    val headerFile = file("$interopDir/$bridgeModuleName.h")
                    val objectFile = swiftOutputDir.file("$bridgeModuleName.o").asFile

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
                        "-sdk",
                        "iphonesimulator",
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
                        "$simulatorArch-apple-ios13.0-simulator",
                        swiftFile.absolutePath,
                    )

                    doLast {
                        logger.info("Successfully compiled $bridgeModuleName Swift bridge for $simulatorArch")
                    }
                }
            }

        // Create an aggregate task that compiles all Swift bridges
        val compileSwiftBridge =
            tasks.register("compileSwiftBridge") {
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
                bridgeModules,
            )

            configureSwiftBridgeLinking(
                listOf(iosSimulatorArm64()),
                bridgeModules,
            )
        }
    }
}

SwiftBridgeConfiguration().configureSwiftBridge()
