// ktlint version constant
val ktlintVersion = "1.7.1"

// Convert ktlint version to IDE plugin format (e.g., "1.7.1" -> "V1_7_1")
fun String.toKtlintIdeVersion(): String = "V${this.replace(".", "_")}"

// Common Kover exclusion patterns
object KoverExclusions {
    val annotations =
        listOf(
            "androidx.compose.ui.tooling.preview.Preview",
            "org.jetbrains.compose.ui.tooling.preview.Preview",
            "network.bisq.mobile.presentation.common.ui.utils.PreviewHelper",
            "network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage",
        )
    val classes =
        listOf(
            "*ComposableSingletons*",
            "bisqapps.*.generated.resources.*",
        )
}

plugins {
    // trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.cocoapods).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.buildconfig).apply(false)
    alias(libs.plugins.protobuf).apply(false)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover) // Apply kover in root for aggregated reports

    // For Java & KotlinMultiplatform/Jvm this is for stripping out unused compilations
    // of tor to reduce application binary size by keeping only the host/architecture
    // necessary for that distribution.
    // See: https://github.com/05nelsonm/kmp-tor-resource/blob/master/library/resource-filterjar-gradle-plugin/README.md
    alias(libs.plugins.kmp.tor.resource.filterjar).apply(false)

    // For iOS device some setup is needed to incorporate the LibTor.framework
    // that is expected to be present at runtime.
    // See: https://github.com/05nelsonm/kmp-tor-resource/blob/master/library/resource-frameworks-gradle-plugin/README.md
    alias(libs.plugins.kmp.tor.resource.frameworks)
}

// Configure kmp-tor-resource-frameworks plugin for iOS
// This generates LibTor.xcframework in build/kmp-tor-resource/
// which must be manually added to the Xcode project
kmpTorResourceFrameworks {
    torGPL.set(true) // We use the -gpl variants
}

// Kover dependencies for aggregated coverage reports
dependencies {
    kover(project(":shared:domain"))
    kover(project(":shared:presentation"))
    kover(project(":apps:clientApp"))
    // Note: shared:kscan is excluded as it's a third-party library wrapper with no tests
    // Note: apps:nodeApp is excluded as it requires Maven secrets not available in fork PRs
}

// Configure all subprojects to run generateResourceBundles before compilation
subprojects {
    // Apply ktlint to all subprojects with KMP plugin
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(
            plugin =
                rootProject.libs.plugins.ktlint
                    .get()
                    .pluginId,
        )

        // Configure ktlint
        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(ktlintVersion)
            verbose.set(true)
            android.set(true)
            outputToConsole.set(true)
            outputColorName.set("RED")
            ignoreFailures.set(false)
            filter {
                exclude("**/generated/**")
                exclude("**/build/**")
                exclude("**/buildConfig/**")
                exclude { element -> element.file.path.contains("GeneratedResourceBundles") }
                exclude { element -> element.file.path.contains("buildConfig") }
                exclude { element -> element.file.path.contains("/build/generated/") }
            }
        }

        // Add Compose Rules as a custom ktlint ruleset
        dependencies {
            add("ktlintRuleset", rootProject.libs.compose.rules.ktlint)
        }
    }

    // Apply kover configuration to all subprojects with the kover plugin
    plugins.withId(
        rootProject.libs.plugins.kover
            .get()
            .pluginId,
    ) {
        configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        KoverExclusions.annotations.forEach { annotatedBy(it) }
                        KoverExclusions.classes.forEach { classes(it) }
                    }
                }
            }
        }
    }

    afterEvaluate {
        // Only apply to projects that have the generateResourceBundles task
        tasks.findByName("generateResourceBundles")?.let { generateTask ->
            // Make all compile-related tasks depend on generateResourceBundles
            tasks
                .matching { task ->
                    task.name.contains("compile", ignoreCase = true) ||
                        task.name.contains("build", ignoreCase = true) ||
                        task.name.startsWith("assemble") ||
                        task.name.startsWith("bundle")
                }.configureEach {
                    dependsOn(generateTask)
                }
        }
    }
}

// ios versioning linking
tasks.register("updatePlist") {
    doLast {
        val plistFile = file("iosClient/iosClient/Info.plist") // Adjust path if needed
        if (!plistFile.exists()) {
            throw GradleException("Info.plist not found at ${plistFile.absolutePath}")
        }

        // Version code should be updated manually on release
        val version = project.findProperty("client.ios.version") as String
        val versionCode = project.findProperty("client.ios.version.code") as String

        val plistContent =
            plistFile
                .readText()
                .replace(
                    "<key>CFBundleShortVersionString</key>\\s*<string>.*?</string>".toRegex(),
                    "<key>CFBundleShortVersionString</key>\n\t<string>$version</string>",
                ).replace(
                    "<key>CFBundleVersion</key>\\s*<string>.*?</string>".toRegex(),
                    "<key>CFBundleVersion</key>\n\t<string>$versionCode</string>",
                )

        plistFile.writeText(plistContent)
        println("Updated Info.plist with version: $version")
    }
}

// Ensure it runs before iOS builds
tasks.matching { it.name.startsWith("link") }.configureEach {
    dependsOn("updatePlist")
}

// Automatically configure Git hooks on project sync/build
tasks.register<Exec>("installGitHooks") {
    description = "Configures Git to use .githooks directory"
    group = "git"

    commandLine(
        "sh",
        "-c",
        """
        if [ -d .git ]; then
            git config core.hooksPath .githooks
            chmod +x .githooks/*
            echo "✅ Git hooks configured automatically"
        fi
        """.trimIndent(),
    )

    isIgnoreExitValue = true // Don't fail build if git is not available
}

// Verify ktlint IDE plugin configuration
abstract class VerifyKtlintIdePluginTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val ktlintPluginFile: RegularFileProperty

    @get:Input
    abstract val expectedVersion: Property<String>

    @TaskAction
    fun verify() {
        val pluginFile = ktlintPluginFile.orNull?.asFile

        if (pluginFile == null || !pluginFile.exists()) {
            println("⚠️  ktlint IDE plugin configuration not found at .idea/ktlint-plugin.xml")
            println("   Please install the ktlint plugin in Android Studio/IntelliJ IDEA")
            println("   Plugin: https://plugins.jetbrains.com/plugin/15057-ktlint")
            return
        }

        val content = pluginFile.readText()

        // Check if plugin is enabled (not DISABLED)
        val isDisabled = content.contains("<ktlintMode>DISABLED</ktlintMode>")

        if (isDisabled) {
            println("ℹ️  ktlint IDE plugin is currently DISABLED")
            println("   To enable it, open Android Studio/IntelliJ IDEA:")
            println("   Settings → Tools → ktlint → Enable ktlint")
            return
        }

        // Plugin is enabled, check version
        val versionPattern = "<ktlintRulesetVersion>([^<]+)</ktlintRulesetVersion>".toRegex()
        val versionMatch = versionPattern.find(content)

        if (versionMatch != null) {
            val configuredVersion = versionMatch.groupValues[1]
            val expected = expectedVersion.get()

            if (configuredVersion == expected) {
                println("✅ ktlint IDE plugin is correctly configured (version: $expected)")
            } else {
                val errorMessage =
                    """
                    |
                    |❌ ktlint IDE plugin version mismatch!
                    |   Expected: $expected
                    |   Configured: $configuredVersion
                    |
                    |   Please update in Android Studio/IntelliJ IDEA:
                    |   Settings → Tools → ktlint → ktlint version → $expected
                    |
                    |   ⚠️  NOTE: If you've already changed the plugin settings and this error
                    |   persists, restart Android Studio for the changes to take effect.
                    |
                    """.trimMargin()
                throw GradleException(errorMessage)
            }
        } else {
            val errorMessage =
                """
                |
                |❌ Could not determine ktlint IDE plugin version from configuration
                |   Please ensure ktlint version is properly configured in:
                |   Settings → Tools → ktlint → ktlint version
                |
                |   ⚠️  NOTE: If you've already changed the plugin settings and this error
                |   persists, restart Android Studio for the changes to take effect.
                |
                """.trimMargin()
            throw GradleException(errorMessage)
        }
    }
}

tasks.register<VerifyKtlintIdePluginTask>("verifyKtlintIdePlugin") {
    description = "Verifies ktlint IDE plugin is configured correctly"
    group = "verification"

    val pluginFile = file(".idea/ktlint-plugin.xml")
    if (pluginFile.exists()) {
        ktlintPluginFile.set(pluginFile)
    }
    expectedVersion.set(ktlintVersion.toKtlintIdeVersion())
}

tasks.register<Exec>("ktlintFormatAndCheck") {
    description = "Formats code with ktlint and fails if any violations remain"
    group = "formatting"

    commandLine("sh", "-c", "./gradlew ktlintFormat --continue && ./gradlew ktlintCheck")
    workingDir(rootProject.projectDir)
}

// Run both installGitHooks and verifyKtlintIdePlugin automatically when project is evaluated
rootProject.tasks.named("prepareKotlinBuildScriptModel").configure {
    dependsOn("installGitHooks", "verifyKtlintIdePlugin")
}

// Configure aggregated Kover reports for the entire repository
kover {
    reports {
        total {
            filters {
                excludes {
                    KoverExclusions.annotations.forEach { annotatedBy(it) }
                    KoverExclusions.classes.forEach { classes(it) }
                }
            }

            html {
                title = "Bisq Mobile - Code Coverage Report"
                onCheck = false
            }

            xml {
                title = "Bisq Mobile - Code Coverage Report"
                onCheck = false
            }

            verify {
                rule("Check Coverage") {
                    bound {
                        // Read minimum coverage from gradle.properties
                        // This ensures coverage doesn't decrease over time
                        minValue = (project.findProperty("kover.coverage.minimum") as? String)?.let { value ->
                            value.toIntOrNull()
                                ?: throw GradleException(
                                    "kover.coverage.minimum in gradle.properties must be an integer (found: '$value')",
                                )
                        } ?: throw GradleException("kover.coverage.minimum property not found in gradle.properties")
                    }
                }
            }
        }
    }
}
