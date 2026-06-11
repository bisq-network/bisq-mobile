import com.android.build.gradle.tasks.factory.AndroidUnitTest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.InputStreamReader
import java.util.Properties
import kotlin.io.path.Path

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.atomicfu)
    alias(libs.plugins.kover)
    // Sentry-KMP plugin must be applied to every module whose iOS test/main
    // binary transitively links Sentry-KMP. This module owns
    // `SentryAnalyticsService` + `DefaultSentryClient` in commonMain, so its
    // K/N test framework needs Sentry.framework on the link path — without
    // the plugin coordinating with cocoapods at this module level the link
    // fails with `ld: framework 'Sentry' not found`.
    alias(libs.plugins.sentry.kotlin.multiplatform)
}

version = project.findProperty("shared.version") as String

// `project.findProperty(...)` reads gradle.properties + -P + env vars but NOT
// the root-level `local.properties` (that file is reserved for the Android
// Gradle plugin's SDK paths). To honour the comment "local.properties
// overrides any property if you need to setup for example local networking"
// below, we load it once here and prefer it over gradle.properties.
//
// Used for: feature.analyticsDevEnabled, analytics.dsn.*, feature.muSigEnabled,
// bisq.isDebug, and any future per-developer override. Never commit
// local.properties — it is gitignored.
val devLocalProperties: Properties =
    Properties().apply {
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { load(it) }
        }
    }

fun resolveProperty(key: String): String? = devLocalProperties.getProperty(key) ?: project.findProperty(key)?.toString()

/**
 * Resolves whether the current build is a Debug build, used by `BuildConfig.IS_DEBUG`
 * and `BuildNodeConfig.IS_DEBUG`.
 *
 * Three detection sources (manual override wins; else any of the auto-detects):
 *  1. **`bisq.isDebug` gradle property** — explicit manual override
 *  2. **Gradle task names containing "debug"** — matches Android Studio invocations
 *     like `:apps:clientApp:assembleDebug`, `installDebug`, etc.
 *  3. **Xcode env vars `CONFIGURATION` / `KOTLIN_FRAMEWORK_BUILD_TYPE`** — required
 *     for iOS builds: Xcode invokes gradle as `embedAndSignAppleFrameworkForXcode`
 *     (always that task name regardless of config), and passes the actual
 *     configuration via env vars set in the build phase. Without this, iOS Debug
 *     builds were incorrectly reporting `IS_DEBUG=false` → `environment=production`
 *     to GlitchTip (analytics phase 0 issue surfaced 2026-06-04).
 *
 * The KMP CocoaPods plugin sets `KOTLIN_FRAMEWORK_BUILD_TYPE` to "DEBUG"/"RELEASE"
 * (uppercase); Xcode itself sets `CONFIGURATION` to "Debug"/"Release". Both are
 * checked for resilience against future plugin / Xcode-version changes.
 */
val isDebugBuild: Boolean =
    project.findProperty("bisq.isDebug")?.toString()?.toBoolean()
        ?: (
            project.gradle.startParameter.taskNames
                .any { it.contains("debug", ignoreCase = true) } ||
                System.getenv("CONFIGURATION").equals("Debug", ignoreCase = true) ||
                System.getenv("KOTLIN_FRAMEWORK_BUILD_TYPE").equals("DEBUG", ignoreCase = true)
        )

val bisqCoreVersion: String by extra {
    findTomlVersion("bisq-core")
}
val bisqApiVersion: String by extra {
    findTomlVersion("bisq-api")
}
val bisqDesktopPairingVersion: String by extra {
    findTomlVersion("bisq-desktop-pairing")
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
        buildConfigField("BISQ_DESKTOP_PAIRING_VERSION", bisqDesktopPairingVersion)
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        // networking setup
        buildConfigField("WS_PORT", project.findProperty("client.x.trustednode.port").toString())
        buildConfigField("WS_ANDROID_HOST", project.findProperty("client.android.trustednode.ip").toString())
        buildConfigField("WS_IOS_HOST", project.findProperty("client.ios.trustednode.ip").toString())
        val muSigEnabled =
            project
                .findProperty("feature.muSigEnabled")
                ?.toString()
                ?.let { value ->
                    value.toBooleanStrictOrNull()
                        ?: error("feature.muSigEnabled must be 'true' or 'false', got '$value'")
                }
                ?: false
        buildConfigField(
            "MU_SIG_ENABLED",
            muSigEnabled,
        )
        // Analytics dev-only override (issue #525). See gradle.properties for the
        // full opt-in story. This is NOT the user-facing gate — that lives in
        // SettingsRepository.analyticsEnabled and is honoured by every build.
        //
        // What this flag does: in DEBUG builds, when false, prevents Sentry from
        // emitting even if the user-settings toggle is ON. Protects production
        // GlitchTip from being polluted by developer test events. In RELEASE
        // builds, this is forced TRUE — end users get a ready-to-flip build.
        //
        // Strict parse: refuses anything that isn't literal "true"/"false" —
        // silent coercion on a privacy-sensitive switch is a foot-gun we
        // explicitly want to avoid. Read via resolveProperty so local.properties
        // takes precedence over gradle.properties.
        val analyticsDevEnabled =
            if (!isDebugBuild) {
                // Release builds always honour the user-settings toggle.
                true
            } else {
                resolveProperty("feature.analyticsDevEnabled")
                    ?.let { value ->
                        value.toBooleanStrictOrNull()
                            ?: error("feature.analyticsDevEnabled must be 'true' or 'false', got '$value'")
                    }
                    ?: false
            }
        buildConfigField("ANALYTICS_DEV_ENABLED", analyticsDevEnabled)
        // DSNs are public per Sentry's threat model — the public key alone
        // cannot read data, only post. Empty default = effectively disabled.
        // Connect's BuildConfig holds both Android + iOS DSNs; the runtime
        // service picks the right one based on getPlatformInfo().
        buildConfigField(
            "ANALYTICS_DSN_ANDROID",
            resolveProperty("analytics.dsn.connect.android").orEmpty(),
        )
        buildConfigField(
            "ANALYTICS_DSN_IOS",
            resolveProperty("analytics.dsn.connect.ios").orEmpty(),
        )
        buildConfigField("IS_DEBUG", isDebugBuild)
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
        // Analytics dev-only override (issue #525). See client BuildConfig above
        // for the full rationale. Node app gets its own DSN pointing at GlitchTip
        // project id=2 (bisq-easy-node-android). Same semantics: release builds
        // always honour the user-settings toggle; debug builds need this flag
        // true to actually emit (dev safety against polluting prod GlitchTip).
        val nodeAnalyticsDevEnabled =
            if (!isDebugBuild) {
                true
            } else {
                resolveProperty("feature.analyticsDevEnabled")
                    ?.let { value ->
                        value.toBooleanStrictOrNull()
                            ?: error("feature.analyticsDevEnabled must be 'true' or 'false', got '$value'")
                    }
                    ?: false
            }
        buildConfigField("ANALYTICS_DEV_ENABLED", nodeAnalyticsDevEnabled)
        buildConfigField(
            "ANALYTICS_DSN",
            resolveProperty("analytics.dsn.node.android").orEmpty(),
        )
        buildConfigField("IS_DEBUG", isDebugBuild)
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

// -------------------- Module References --------------------
val sharedTestUtilsModule = ":shared:test-utils"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    // The Sentry Cocoa SDK pod is declared here (matching apps/clientApp) so
    // the K/N linker for this module's iOS test framework finds Sentry.framework.
    // Sentry-KMP's cinterop bindings reference Sentry Cocoa symbols at link
    // time; without this declaration `linkDebugTestIosSimulatorArm64` fails
    // with `ld: framework 'Sentry' not found`. Shares the iosClient/Podfile
    // with the host app, so a single `pod install` covers every module.
    cocoapods {
        summary = "Bisq Mobile — shared domain module"
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
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
            implementation(libs.androidx.security.crypto)
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
            implementation(libs.kphonenumber)
            api(libs.okio) // api to allow platform specific path conversion for kmp-tor

            // Opt-in analytics SDK (issue #525). Always on the classpath AND
            // always bound in DI now — the SDK ships linked into every release
            // build but stays inert until both runtime gates pass: the dev-only
            // BuildConfig.ANALYTICS_DEV_ENABLED (always true in release) AND
            // SettingsRepository.analyticsEnabled (default OFF, user-controlled).
            // Trade-off: a few hundred KB binary cost in exchange for end users
            // getting a ready-to-flip release build (no rebuild needed to enable).
            implementation(libs.sentry.kotlin.multiplatform)

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

            // Test utilities
            implementation(project(sharedTestUtilsModule))
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

        val languageCodes = listOf("en", "af_ZA", "cs", "de", "es", "fr", "hi", "id", "it", "pcm", "pt_BR", "ru", "tr", "vi")

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
                        "$simulatorArch-apple-ios16.0-simulator",
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
        // Also ensure Swift bridge objects are built before linking iOS main binaries (for frameworks)
        tasks.matching { it.name.startsWith("link") && it.name.contains("IosSimulatorArm64") && !it.name.contains("Test") }.configureEach {
            dependsOn(compileSwiftBridge)
        }
        tasks.matching { it.name.startsWith("link") && it.name.contains("IosArm64") && !it.name.contains("Test") }.configureEach {
            dependsOn(compileSwiftBridge)
        }
        // Also tie to test Kotlin compilation as a safety net (ensures object files exist by link time)
        tasks.matching { it.name == "compileTestKotlinIosSimulatorArm64" }.configureEach {
            dependsOn(compileSwiftBridge)
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
