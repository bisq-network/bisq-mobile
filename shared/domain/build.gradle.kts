import java.io.InputStreamReader
import java.util.Properties
import kotlin.io.path.Path

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.buildconfig)
    kotlin("plugin.serialization") version "2.0.21"
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
            project.findProperty("client.android.version").toString()
        )
        buildConfigField("IOS_APP_VERSION", project.findProperty("client.ios.version").toString())
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BISQ_API_VERSION", bisqApiVersion)
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        // networking setup
        buildConfigField("WS_PORT", project.findProperty("client.x.trustednode.port").toString())
        buildConfigField("WS_ANDROID_HOST", project.findProperty("client.android.trustednode.ip").toString())
        buildConfigField("WS_IOS_HOST", project.findProperty("client.ios.trustednode.ip").toString())
        buildConfigField("IS_DEBUG", project.gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) })
    }
    forClass("network.bisq.mobile.android.node", className = "BuildNodeConfig") {
        buildConfigField("APP_NAME", project.findProperty("node.name").toString())
        buildConfigField("APP_VERSION", project.findProperty("node.android.version").toString())
        buildConfigField("TRADE_PROTOCOL_VERSION", "1.0") // TODO review values
        buildConfigField("TRADE_OFFER_VERSION", 1) // TODO review values
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        buildConfigField("BISQ_CORE_VERSION", bisqCoreVersion)
        buildConfigField("IS_DEBUG", project.gradle.startParameter.taskNames.any { it.contains("debug", ignoreCase = true) })

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
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared Domain business logic and KOJOs"
        homepage = "X"
        version = project.version.toString()
        ios.deploymentTarget = "16.0"
        podfile = project.file("../../iosClient/Podfile")
        framework {
            baseName = "domain"
            isStatic = false
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.logging.kermit)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bignum)

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.json)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.jetbrains.serialization.gradle.plugin)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.websockets)

            implementation(libs.multiplatform.settings)

            implementation(libs.atomicfu)
            implementation(libs.jetbrains.kotlin.reflect)

            configurations.all {
                exclude(group = "org.slf4j", module = "slf4j-api")
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)

            implementation(libs.koin.core)
            implementation(libs.koin.android)
        }
        androidUnitTest.dependencies {
            implementation(libs.mock.io)
            implementation(libs.kotlin.test.junit.v180)
            implementation(libs.junit)

            implementation(libs.roboelectric)
            implementation(libs.androidx.test)
            implementation(libs.androidx.test.espresso)
            implementation(libs.androidx.test.junit)

//            implementation("com.russhwolf:multiplatform-settings-datastore:1.2.0")
//
//            implementation("androidx.test:core:1.5.0")
//            implementation("androidx.test.ext:junit:1.1.5")
//            implementation("androidx.test.espresso:espresso-core:3.5.1")
//            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        }

        iosMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}

android {
    namespace = "network.bisq.mobile.shared.domain"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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

        val bundleNames: List<String> = listOf(
            "default",
            "application",
            "authorized_role",
            "bisq_easy",
            "reputation",
            "chat",
            "support",
            "user",
            "network",
            "settings",
            "payment_method",
            "mobile" // custom for mobile client
        )

        val languageCodes = listOf("en", "af_ZA", "cs", "de", "es", "it", "pcm", "pt_BR", "ru")

        val bundlesByCode: Map<String, List<ResourceBundle>> = languageCodes.associateWith { languageCode ->
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

                val map = properties.entries.associate { it.key.toString() to it.value.toString() }
                ResourceBundle(map, bundleName, languageCode)
            }
        }

        bundlesByCode.forEach { (languageCode, bundles) ->
            val outputFile: File = outputDir.get().file("GeneratedResourceBundles_$languageCode.kt").asFile
            val generatedCode = StringBuilder().apply {
                appendLine("package network.bisq.mobile.i18n")
                appendLine()
                appendLine("// Auto-generated file. Do not modify manually.")
                appendLine("object GeneratedResourceBundles_$languageCode {")
                appendLine("    val bundles = mapOf(")
                bundles.forEach { bundle ->
                    appendLine("        \"${bundle.bundleName}\" to mapOf(")
                    bundle.map.forEach { (key, value) ->
                        val escapedValue = value
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

    data class ResourceBundle(val map: Map<String, String>, val bundleName: String, val languageCode: String)
}

tasks.register<GenerateResourceBundlesTask>("generateResourceBundles") {
    group = "build"
    description = "Generate a Kotlin file with hardcoded ResourceBundle data"
    inputDir.set(layout.projectDirectory.dir("src/commonMain/resources/mobile"))
    // Using build dir still not working on iOS
    // Thus we use the source dir as target
    outputDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/network/bisq/mobile/i18n"))
}

fun findTomlVersion(versionName: String): String {
    val tomlFile = file("../../gradle/libs.versions.toml")
    val tomlContent = tomlFile.readText()
    val versionRegex = Regex("$versionName\\s*=\\s*\"([^\"]+)\"")
    val matchResult = versionRegex.find(tomlContent)
    return matchResult?.groups?.get(1)?.value ?: "unknown"
}