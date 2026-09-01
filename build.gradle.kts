// R8 pinned ahead of the AGP-bundled one: AGP 8.13.x ships an R8 whose kotlin-metadata parser
// predates Kotlin 2.4, so every minified build warns "error occurred when parsing kotlin
// metadata" and Kotlin-aware shrinking silently degrades to plain-Java treatment. Drop this
// pin when AGP is upgraded to a version whose bundled R8 understands the project's Kotlin.
// Compatibility table: https://developer.android.com/studio/build/kotlin-d8-r8-versions
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
//        FIXME causing initial data process to get stuck forever - seems to be D8/R8 desugaring issue
//        classpath("com.android.tools:r8:9.4.12")
    }
}

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
            "network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage",
        )
    val classes =
        listOf(
            "*ComposableSingletons*",
            "bisqapps.*.generated.resources.*",
            // Auto-generated i18n resource bundles (one giant `mapOf(...)` per language,
            // produced by the generateResourceBundles task from mobile/*.properties). They are
            // generated data tables, not logic — the same rationale as the Compose generated
            // resources above. Beyond that, once a translation batch grows a bundle's initializer
            // past the coverage engine's per-method instrumentation size limit so this is a key one
            "network.bisq.mobile.i18n.GeneratedResourceBundles*",
            "network.bisq.mobile.presentation.design.*",
            // Excluding here so the diff coverage gate reflects code we can
            // actually test in isolation.
            "network.bisq.mobile.domain.analytics.DefaultSentryClient*",
            "network.bisq.mobile.domain.analytics.NoOpAnalyticsService",
            // Android Keystore adapters: Robolectric ships no AndroidKeyStore provider
            // (KeyStore.getInstance("AndroidKeyStore") throws NoSuchAlgorithmException), so
            // these run on a device only. Test changes to them in
            // PushNotificationKeyStoreInstrumentedTest; do not try to unit test them.
            "network.bisq.mobile.data.crypto.KeystoreKeyWrapper",
            // Delete this line together with LegacyPushNotificationKeyCleanup.kt.
            "network.bisq.mobile.data.crypto.LegacyMasterKey",
            // Koin DI modules — pure declarative wiring factories
            "network.bisq.mobile.client.common.di.*",
            "network.bisq.mobile.node.common.di.*",
            "network.bisq.mobile.data.di.*",
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
    alias(libs.plugins.google.services).apply(false) // applied in :apps:clientApp for FCM

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
    // KGP's cinterop commonizer invokes `Task.project` at execution time — a configuration-cache
    // violation inside the Kotlin plugin (surfaces as "compileIosMainKotlinMetadata caused
    // invocation of 'Task.project' by commonizeCInterop") that FAILS the build at the very end.
    // Marking the offenders incompatible downgrades that to discarding the cache entry: iOS task
    // graphs can't cache anyway (Swift-bridge Exec tasks), Android-only builds keep caching.
    // Re-check when upgrading Kotlin — if KGP fixes it, this block can go.
    tasks.configureEach {
        if (name == "commonizeCInterop" || name == "compileIosMainKotlinMetadata") {
            notCompatibleWithConfigurationCache("KGP commonizeCInterop accesses Task.project at execution time")
        }
    }

    // #1363: Rewrite JetBrains dual-publish aliases → AndroidX on *metadata* classpaths.
    // This deliberately covers native source-set metadata (iosMain/nativeMain/appleMain) too:
    // rewriting commonMain while leaving nativeMain on org.jetbrains.* is precisely what puts
    // two klibs with the same unique_name (e.g. runtime_commonMain) on one classpath.
    // Platform klib classpaths (iosArm64CompileKlibraries) carry no "Metadata" in their names,
    // so iOS binaries still link org.jetbrains.* — compose.ui depends on both.
    val jetbrainsToAndroidx =
        mapOf(
            "org.jetbrains.compose.runtime" to
                (
                    "androidx.compose.runtime" to
                        rootProject.libs.versions.androidx.compose.runtime
                            .get()
                ),
            "org.jetbrains.androidx.lifecycle" to
                (
                    "androidx.lifecycle" to
                        rootProject.libs.versions.androidx.lifecycle
                            .get()
                ),
            "org.jetbrains.androidx.savedstate" to
                (
                    "androidx.savedstate" to
                        rootProject.libs.versions.androidx.savedstate
                            .get()
                ),
            "org.jetbrains.compose.annotation-internal" to
                (
                    "androidx.annotation" to
                        rootProject.libs.versions.androidx.annotation
                            .get()
                ),
            "org.jetbrains.compose.collection-internal" to
                (
                    "androidx.collection" to
                        rootProject.libs.versions.androidx.collection
                            .get()
                ),
        )
    // Root alias modules only — skip runtime-iosarm64 / *-uikitarm64 / etc.
    val platformArtifact =
        Regex("(?i)-(ios|uikit|android|jvm|desktop|macos|linux|js|wasm|mingw|watchos|tvos)[a-z0-9]*$")
    val renamedModules =
        mapOf(
            "org.jetbrains.compose.annotation-internal" to "annotation",
            "org.jetbrains.compose.collection-internal" to "collection",
        )
    // Don't narrow this name match: `allSourceSetsCompileDependenciesMetadata` is KGP's
    // reference configuration for consistent resolution, so leaving it out makes the strict
    // versions it publishes contradict the substituted ones and resolution fails outright.
    // Removal trigger: `contains("Metadata")` is a string match on KGP config names, so
    // retest Android `releaseRuntimeClasspath` on every Kotlin / CMP bump. Delete this
    // whole block when CMP stops dual-publishing the same klib unique_name under both
    // org.jetbrains.* and androidx.*.
    configurations.configureEach {
        if (!name.contains("Metadata", ignoreCase = true)) return@configureEach

        resolutionStrategy.eachDependency {
            val group = requested.group ?: return@eachDependency
            val (toGroup, version) = jetbrainsToAndroidx[group] ?: return@eachDependency
            if (platformArtifact.containsMatchIn(requested.name)) return@eachDependency
            val toModule = renamedModules[group] ?: requested.name
            useTarget("$toGroup:$toModule:$version")
            because("Prefer AndroidX over JetBrains alias on metadata classpath (#1363)")
        }
    }

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
            currentProject {
                instrumentation {
                    disabledForTestTasks.add("testReleaseUnitTest")
                }
            }

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

tasks.register<Exec>("koverPRCheck") {
    description = "Runs Kover to check this branch coverage and fails if coverage is below the threshold"
    group = "verification"

    commandLine("sh", "-c", "./gradlew koverXmlReport && ./scripts/analyze-diff-coverage.sh upstream/main build/reports/kover/report.xml")
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
