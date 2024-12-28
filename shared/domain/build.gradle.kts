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
        buildConfigField("BUILD_TS", System.currentTimeMillis())
        // networking setup
        buildConfigField("WS_PORT", project.findProperty("client.x.trustednode.port").toString())
        buildConfigField("WS_ANDROID_HOST", project.findProperty("client.android.trustednode.ip").toString())
        buildConfigField("WS_IOS_HOST", project.findProperty("client.ios.trustednode.ip").toString())
    }
    forClass("network.bisq.mobile.android.node", className = "BuildNodeConfig") {
        buildConfigField("APP_NAME", project.findProperty("node.name").toString())
        buildConfigField("APP_VERSION", project.findProperty("node.android.version").toString())
        buildConfigField("SHARED_LIBS_VERSION", project.version.toString())
        buildConfigField("BUILD_TS", System.currentTimeMillis())
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
        val commonMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("generated/commonMain/kotlin"))
//            kotlin.src(layout.buildDirectory.file("generated/commonMain/kotlin/network/bisq/mobile/i18n/GeneratedResourceBundles.kt"))
        }

        val androidMain by getting {
            dependsOn(commonMain)
        }
        val iosX64Main by getting {
            dependsOn(commonMain)
        }
        val iosArm64Main by getting {
            dependsOn(commonMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(commonMain)
        }

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

    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
// Create a task class to ensure proper serialization for configuration cache compatibility
abstract class GenerateResourceBundlesTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val resourceDir = inputDir.asFile.get()
        val outputFile = outputFile.asFile.get()
        val outputDir = outputFile.parentFile

        val bundleNames: List<String> = listOf(
            "default", "application", "bisq_easy", "reputation", "chat", "support",
            "user", "network", "settings", "payment_method", "offer", "mobile"
        )
        val languageCodes = listOf("en", "af_ZA", "cs", "de", "es", "it", "pcm", "pt_BR", "ru")

        // Generate a class for each language code
        languageCodes.forEach { languageCode ->
            generateLanguageClass(resourceDir, outputDir, bundleNames, languageCode)
        }

        // Generate the main class that references all language classes
        generateMainClass(outputFile, languageCodes)
    }

    private fun generateLanguageClass(resourceDir: File, outputDir: File, bundleNames: List<String>, languageCode: String) {
        val bundles = bundleNames.map { bundleName ->
            val code = if (languageCode.lowercase() == "en") "" else "_$languageCode"
            val fileName = "$bundleName$code.properties"
            var file = Path(resourceDir.path, fileName).toFile()
            if (!file.exists()) {
                file = Path(resourceDir.path, "$bundleName.properties").toFile()
            }
            val properties = Properties()
            properties.load(file.inputStream())
            val map = properties.entries.associate { it.key.toString() to it.value.toString() }
            ResourceBundle(map, bundleName, languageCode)
        }

        val className = "GeneratedResourceBundle_${languageCode.replace("-", "_")}"
        val generatedCode = StringBuilder().apply {
            appendLine("package network.bisq.mobile.i18n")
            appendLine()
            appendLine("// Auto-generated file. Do not modify manually.")
            appendLine("internal object $className {")
            appendLine("    val bundles = mapOf(")
            bundles.forEach { bundle ->
                appendLine("        \"${bundle.bundleName}\" to mapOf(")
                bundle.map.forEach { (key, value) ->
                    val escapedValue = value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                    appendLine("            \"$key\" to \"$escapedValue\",")
                }
                appendLine("        ),")
            }
            appendLine("    )")
            appendLine("}")
        }

        val outputFile = File(outputDir, "$className.kt")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedCode.toString())
    }

    private fun generateMainClass(outputFile: File, languageCodes: List<String>) {
        val generatedCode = StringBuilder().apply {
            appendLine("package network.bisq.mobile.i18n")
            appendLine()
            appendLine("// Auto-generated file. Do not modify manually.")
            appendLine("object GeneratedResourceBundles {")
            appendLine("    val bundlesByCode = mapOf(")
            languageCodes.forEach { languageCode ->
                val className = "GeneratedResourceBundle_${languageCode.replace("-", "_")}"
                appendLine("        \"$languageCode\" to $className.bundles,")
            }
            appendLine("    )")
            appendLine("}")
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(generatedCode.toString())
    }

    data class ResourceBundle(val map: Map<String, String>, val bundleName: String, val languageCode: String)
}

tasks.register<GenerateResourceBundlesTask>("generateResourceBundles") {
    group = "build"
    description = "Generate a Kotlin file with hardcoded ResourceBundle data"
    inputDir.set(layout.projectDirectory.dir("src/commonMain/resources/mobile"))
    outputFile.set(layout.buildDirectory.file("generated/commonMain/kotlin/network/bisq/mobile/i18n/GeneratedResourceBundles.kt"))
    outputs.file(outputFile)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    mustRunAfter("generateResourceBundles")
}

//tasks.named("compileCommonMainKotlinMetadata") {
//    dependsOn("generateResourceBundles")
//}

kotlin.sourceSets["commonMain"].kotlin.srcDir(tasks.named("generateResourceBundles"))