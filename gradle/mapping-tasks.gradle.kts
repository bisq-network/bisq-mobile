/**
 * Shared Gradle script for configuring ProGuard mapping file management.
 * This script creates tasks to automatically save mapping files, configuration, and metadata
 * after successful release builds.
 *
 * Usage:
 * In module build.gradle.kts:
 * extra["moduleName"] = "nodeApp" // or "clientApp"
 * apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")
 *
 * A module without product flavors saves its files to mappings/<module>/. A module with flavors
 * saves them to mappings/<module>/<flavor>/, because every flavor has its own mapping.
 */

// Get the module name from extra properties
val moduleName =
    project.extra["moduleName"] as? String
        ?: throw GradleException("Module name must be set via extra['moduleName'] before applying mapping-tasks.gradle.kts")

// Validate module name
require(moduleName.isNotBlank()) { "Module name cannot be blank" }

// Map new module names to legacy directory names for continuity of mappings
val legacyModuleDirName =
    when (moduleName) {
        "nodeApp" -> "androidNode"
        "clientApp" -> "androidClient"
        else -> moduleName
    }

// The task actions use only this object and values captured at configuration time, which keeps
// them compatible with the configuration cache (an action must not reference the script itself).
object MappingFiles {
    const val ARCHIVE_NAME = "mapping.txt.tar.gz"
    private const val RELEASE_VARIANT_SUFFIX = "Release"
    private const val UNFLAVORED_RELEASE_VARIANT = "release"

    // build/outputs/mapping holds one directory per variant: "release", or "<flavor>Release"
    fun releaseVariantDirs(mappingOutputsDir: File): List<File> =
        mappingOutputsDir
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && (it.name == UNFLAVORED_RELEASE_VARIANT || it.name.endsWith(RELEASE_VARIANT_SUFFIX)) }
            .sortedBy { it.name }

    // "release" -> null, "googleRelease" -> "google"
    fun flavorOf(variantDirName: String): String? =
        variantDirName.removeSuffix(RELEASE_VARIANT_SUFFIX).takeIf { it != variantDirName && it.isNotEmpty() }

    fun flavorDir(
        mappingsDir: File,
        flavor: String?,
    ): File = if (flavor == null) mappingsDir else File(mappingsDir, flavor)

    fun runCommand(
        command: List<String>,
        workingDir: File? = null,
        environment: Map<String, String> = emptyMap(),
    ): Pair<Int, String> {
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        workingDir?.let { builder.directory(it) }
        builder.environment().putAll(environment)
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        return process.waitFor() to output
    }
}

// Task to save ProGuard mapping files after successful release builds
tasks.register("saveMappingFiles") {
    group = "bisq"
    description = "Save ProGuard mapping files to mappings/$legacyModuleDirName directory"

    // Configuration cache compatible - capture values at configuration time
    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val outputsDirProvider = layout.buildDirectory.dir("outputs")
    val moduleNameForLogging = legacyModuleDirName

    doLast {
        val mappingsDir = File(mappingsDirPath)
        val variantDirs = MappingFiles.releaseVariantDirs(File(outputsDirProvider.get().asFile, "mapping"))
        if (variantDirs.isEmpty()) {
            println("[$moduleNameForLogging] No release mapping found. Run a release build with R8 first.")
        }

        variantDirs.forEach { variantDir ->
            val targetDir = MappingFiles.flavorDir(mappingsDir, MappingFiles.flavorOf(variantDir.name))
            targetDir.mkdirs()

            // Copy configuration.txt
            val configFile = File(variantDir, "configuration.txt")
            if (configFile.exists()) {
                configFile.copyTo(File(targetDir, "configuration.txt"), overwrite = true)
                println("[$moduleNameForLogging] Saved configuration.txt to $targetDir")
            } else {
                println("[$moduleNameForLogging] configuration.txt not found at ${configFile.absolutePath}")
            }

            // Compress and copy mapping.txt as tar.gz
            val mappingFile = File(variantDir, "mapping.txt")
            if (mappingFile.exists()) {
                val tarGzFile = File(targetDir, MappingFiles.ARCHIVE_NAME)
                val (exitCode, output) =
                    MappingFiles.runCommand(listOf("tar", "-czf", tarGzFile.absolutePath, "-C", mappingFile.parent, mappingFile.name))
                if (exitCode == 0) {
                    println("[$moduleNameForLogging] Saved ${MappingFiles.ARCHIVE_NAME} to $targetDir")
                } else {
                    throw GradleException("[$moduleNameForLogging] Failed to create ${MappingFiles.ARCHIVE_NAME}: $output")
                }
            } else {
                println("[$moduleNameForLogging] mapping.txt not found at ${mappingFile.absolutePath}")
            }
        }
    }
}

// Task to save output metadata after assembleRelease
tasks.register("saveOutputMetadata") {
    group = "bisq"
    description = "Save output-metadata.json to mappings/$legacyModuleDirName directory"

    // Configuration cache compatible - capture values at configuration time
    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val outputsDirProvider = layout.buildDirectory.dir("outputs")
    val moduleNameForLogging = legacyModuleDirName

    doLast {
        val mappingsDir = File(mappingsDirPath)
        val buildOutputsDir = outputsDirProvider.get().asFile

        MappingFiles.releaseVariantDirs(File(buildOutputsDir, "mapping")).forEach { variantDir ->
            val flavor = MappingFiles.flavorOf(variantDir.name)
            val targetDir = MappingFiles.flavorDir(mappingsDir, flavor)
            targetDir.mkdirs()

            // Copy output-metadata.json from either APK or Bundle output location
            val apkDir = if (flavor == null) "apk/release" else "apk/$flavor/release"
            val apkMetadata = File(buildOutputsDir, "$apkDir/output-metadata.json")
            val bundleMetadata = File(buildOutputsDir, "bundle/${variantDir.name}/output-metadata.json")
            val source = listOf(apkMetadata, bundleMetadata).firstOrNull { it.exists() }
            if (source != null) {
                source.copyTo(File(targetDir, "output-metadata.json"), overwrite = true)
                println("[$moduleNameForLogging] Saved output-metadata.json to $targetDir")
            } else {
                println(
                    "[$moduleNameForLogging] output-metadata.json not found at ${apkMetadata.absolutePath} " +
                        "or ${bundleMetadata.absolutePath}",
                )
            }
        }
    }
}

// Task to upload mapping files to a GitHub release as assets.
// Usage: ./gradlew :apps:clientApp:uploadMappings -PreleaseTag=connect_0.3.0
//        ./gradlew :apps:nodeApp:uploadMappings -PreleaseTag=anode_0.5.0
tasks.register("uploadMappings") {
    group = "bisq"
    description = "Upload mapping files from mappings/$legacyModuleDirName to a GitHub release"
    // Never zip files which the save tasks are still writing
    mustRunAfter("saveMappingFiles", "saveOutputMetadata")

    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val moduleNameForLogging = legacyModuleDirName
    val repo = "bisq-network/bisq-mobile"
    // Capture at configuration time for configuration cache compatibility
    val releaseTag = providers.gradleProperty("releaseTag")

    doLast {
        val tag =
            releaseTag.orNull
                ?: throw GradleException("Required property 'releaseTag' not set. Usage: -PreleaseTag=connect_0.3.0")

        val mappingsDir = File(mappingsDirPath)
        val zipName = "$moduleNameForLogging-mappings.zip"
        val zipFile = File(mappingsDir, zipName)
        // A zip of an earlier run must not end up inside the new one
        zipFile.delete()

        val filesToZip = mappingsDir.walkTopDown().filter { it.isFile }.toList()
        if (filesToZip.none { it.name == MappingFiles.ARCHIVE_NAME }) {
            throw GradleException("[$moduleNameForLogging] No mapping files found in $mappingsDir. Run a release build first.")
        }

        // Paths relative to the mappings directory keep the flavor directories apart in the zip
        val zipCommand = listOf("zip", zipFile.absolutePath) + filesToZip.map { it.relativeTo(mappingsDir).path }
        val (zipExit, zipOutput) = MappingFiles.runCommand(zipCommand, workingDir = mappingsDir)
        if (zipExit != 0) {
            throw GradleException("[$moduleNameForLogging] Failed to create $zipName: $zipOutput")
        }
        println("[$moduleNameForLogging] Created $zipName (${zipFile.length() / 1024}KB)")

        // Upload zip to GitHub release
        val (exitCode, output) =
            MappingFiles.runCommand(listOf("gh", "release", "upload", tag, zipFile.absolutePath, "--repo", repo, "--clobber"))
        if (exitCode == 0) {
            println("[$moduleNameForLogging] Uploaded $zipName to release '$tag'")
        } else {
            throw GradleException("[$moduleNameForLogging] Failed to upload $zipName: $output")
        }
    }
}

// Hook after release builds without afterEvaluate; works even if tasks are created later.
// Matches assembleRelease and bundleRelease, and their per-flavor tasks like assembleGoogleRelease.
val releaseBuildTaskName = Regex("(assemble|bundle)([A-Z][A-Za-z0-9]*)?Release")
tasks.matching { releaseBuildTaskName.matches(it.name) }.configureEach {
    finalizedBy("saveMappingFiles")
    finalizedBy("saveOutputMetadata")
}

// -----------------------------------------------------------------------------
// GlitchTip mapping upload (opt-in analytics)
//
// Uploads the saved Proguard mapping.txt to the per-app GlitchTip project via
// sentry-cli, routed through the local Tor SOCKS5 proxy (the GlitchTip server
// is a Tor onion only). The task is OPT-IN — it skips silently when:
//   - sentry.properties is absent (no auth token configured), OR
//   - sentry-cli isn't installed on PATH.
// This means committers without a sentry.properties never see this task error;
// it just doesn't run.
//
// Usage:
//   ./gradlew :apps:clientApp:saveMappingFiles  # produces mapping.txt.tar.gz
//   ./gradlew :apps:clientApp:uploadMappingsToGlitchTip -PreleaseTag=connect_0.4.1
//   A module with flavors also needs the flavor of the released build:
//   ./gradlew :apps:clientApp:uploadMappingsToGlitchTip -PreleaseTag=connect_0.4.1 -PmappingFlavor=google
//
// Prerequisites:
//   - sentry-cli installed:   brew install getsentry/tools/sentry-cli
//   - Tor running locally:    socks5 on 127.0.0.1:9050
//   - sentry.properties:      copy from sentry.properties.template, add auth.token
//
// Why not the Sentry Android Gradle plugin: it auto-uploads on every release
// build and assumes clearnet. For now wants explicit, Tor-routed, user-invoked
// uploads. TODO The Gradle plugin can be added when production releases
// are automated.
// -----------------------------------------------------------------------------
tasks.register("uploadMappingsToGlitchTip") {
    group = "bisq"
    description = "Upload mapping.txt to GlitchTip via sentry-cli over Tor"

    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val sentryPropertiesPath = "${project.projectDir}/sentry.properties"
    val moduleNameForLogging = legacyModuleDirName
    val releaseTagProperty = providers.gradleProperty("releaseTag")
    val mappingFlavorProperty = providers.gradleProperty("mappingFlavor")
    val uploadDirProvider = layout.buildDirectory.dir("glitchtip-upload")

    doLast {
        val sentryProperties = File(sentryPropertiesPath)
        if (!sentryProperties.exists()) {
            println(
                "[$moduleNameForLogging] Skipping GlitchTip upload — $sentryPropertiesPath not " +
                    "present. Copy sentry.properties.template, fill in auth.token, then re-run.",
            )
            return@doLast
        }
        val releaseTag =
            releaseTagProperty.orNull
                ?: throw GradleException(
                    "Required property 'releaseTag' not set. " +
                        "Usage: -PreleaseTag=connect_0.4.1",
                )

        // Check sentry-cli on PATH — soft fail with a helpful message rather
        // than a stack trace from ProcessBuilder.
        val (whichExit, _) = MappingFiles.runCommand(listOf("sh", "-c", "command -v sentry-cli"))
        if (whichExit != 0) {
            println(
                "[$moduleNameForLogging] Skipping GlitchTip upload — sentry-cli not on PATH. " +
                    "Install with: brew install getsentry/tools/sentry-cli",
            )
            return@doLast
        }

        // The saved mapping is gzip-tar from saveMappingFiles. sentry-cli wants
        // the raw mapping.txt — extract to a temp location.
        val tarGz = File(MappingFiles.flavorDir(File(mappingsDirPath), mappingFlavorProperty.orNull), MappingFiles.ARCHIVE_NAME)
        if (!tarGz.exists()) {
            throw GradleException(
                "[$moduleNameForLogging] $tarGz not found. Run saveMappingFiles first " +
                    "(or assemble/bundleRelease which finalizes on it). A module with flavors needs " +
                    "-PmappingFlavor=<flavor>.",
            )
        }
        val tmpDir = uploadDirProvider.get().asFile
        tmpDir.mkdirs()
        val (extractExit, extractOutput) = MappingFiles.runCommand(listOf("tar", "-xzf", tarGz.absolutePath, "-C", tmpDir.absolutePath))
        if (extractExit != 0) {
            throw GradleException("[$moduleNameForLogging] Failed to extract $tarGz: $extractOutput")
        }
        val mappingTxt = File(tmpDir, "mapping.txt")
        if (!mappingTxt.exists()) {
            throw GradleException("[$moduleNameForLogging] mapping.txt missing in extracted archive")
        }

        // Invoke sentry-cli through Tor. The HTTPS_PROXY env var works for
        // sentry-cli's reqwest HTTP client; socks5h:// makes DNS happen via
        // Tor too, which is required for .onion resolution.
        val env =
            mapOf(
                "HTTPS_PROXY" to "socks5h://127.0.0.1:9050",
                "HTTP_PROXY" to "socks5h://127.0.0.1:9050",
                "SENTRY_PROPERTIES" to sentryProperties.absolutePath,
            )

        // Create release (idempotent), upload proguard mapping, finalize.
        // sentry-cli's upload-proguard auto-extracts the proguard UUID from the
        // mapping file header so we don't need to pass it explicitly.
        val steps =
            listOf(
                listOf("sentry-cli", "releases", "new", releaseTag),
                listOf("sentry-cli", "upload-proguard", mappingTxt.absolutePath),
                listOf("sentry-cli", "releases", "finalize", releaseTag),
            )

        for (cmd in steps) {
            val (exit, output) = MappingFiles.runCommand(cmd, environment = env)
            if (exit != 0) {
                throw GradleException(
                    "[$moduleNameForLogging] sentry-cli command failed: ${cmd.joinToString(" ")}\n$output",
                )
            }
            output.lineSequence().filter { it.isNotBlank() }.forEach {
                println("[$moduleNameForLogging] $it")
            }
        }
        println("[$moduleNameForLogging] Uploaded mapping for release '$releaseTag' to GlitchTip")
    }
}
