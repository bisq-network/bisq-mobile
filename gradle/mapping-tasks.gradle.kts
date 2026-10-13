import java.util.Properties
import java.util.UUID

/**
 * Shared Gradle script for configuring ProGuard mapping file management.
 * This script creates tasks to automatically save mapping files, configuration, and metadata
 * after successful release builds.
 *
 * Usage:
 * In module build.gradle.kts:
 * extra["moduleName"] = "nodeApp" // or "clientApp"
 * extra["mappingFlavors"] = listOf("google", "fdroid") // empty for a module without flavors
 * extra["mappingVersionCode"] = versionCodeValue
 * apply(from = "$rootDir/gradle/mapping-tasks.gradle.kts")
 *
 * A module without product flavors saves its files to mappings/<module>/. A module with flavors
 * saves them to mappings/<module>/<flavor>/, because every flavor has its own mapping.
 *
 * Every saved mapping gets an identity file which names the release it belongs to: module,
 * variant, version and the ProGuard UUID. The upload tasks refuse a mapping whose identity is not
 * the one of the current release, so a mapping of an earlier build cannot be published for it.
 *
 * The ProGuard UUID is what binds an obfuscated stack trace to its mapping: the app carries it in
 * its manifest (see the module build files, which get it from `proguardUuidOfFlavor`), and the
 * upload to GlitchTip registers the mapping under the same UUID. It is derived from the identity
 * of the release, not random, so a build of the same source stays reproducible. A rebuild which
 * gets released needs a new version code, otherwise two mappings share one UUID.
 */

// Get the module name from extra properties
val moduleName =
    project.extra["moduleName"] as? String
        ?: throw GradleException("Module name must be set via extra['moduleName'] before applying mapping-tasks.gradle.kts")

// Validate module name
require(moduleName.isNotBlank()) { "Module name cannot be blank" }

val mappingFlavors: List<String> =
    (project.extra["mappingFlavors"] as? List<*>)?.map { it.toString() }
        ?: throw GradleException("Flavors must be set via extra['mappingFlavors'] before applying mapping-tasks.gradle.kts")

val mappingVersionCode =
    project.extra["mappingVersionCode"] as? Int
        ?: throw GradleException("Version code must be set via extra['mappingVersionCode'] before applying mapping-tasks.gradle.kts")

val mappingVersionName = project.version.toString()

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
    const val CONFIGURATION_NAME = "configuration.txt"
    const val OUTPUT_METADATA_NAME = "output-metadata.json"
    const val IDENTITY_NAME = "mapping-identity.properties"
    const val UUID_KEY = "proguardUuid"
    private const val UNFLAVORED_RELEASE_VARIANT = "release"

    val savedFileNames = listOf(ARCHIVE_NAME, CONFIGURATION_NAME, OUTPUT_METADATA_NAME, IDENTITY_NAME)

    // null -> "release", "google" -> "googleRelease"
    fun variantName(flavor: String?): String = if (flavor == null) UNFLAVORED_RELEASE_VARIANT else "${flavor}Release"

    fun flavorDir(
        mappingsDir: File,
        flavor: String?,
    ): File = if (flavor == null) mappingsDir else File(mappingsDir, flavor)

    fun proguardUuid(
        module: String,
        flavor: String?,
        versionName: String,
        versionCode: Int,
    ): String = UUID.nameUUIDFromBytes("bisq-mobile:$module:${variantName(flavor)}:$versionName:$versionCode".toByteArray()).toString()

    fun identity(
        module: String,
        flavor: String?,
        versionName: String,
        versionCode: Int,
    ): Map<String, String> =
        linkedMapOf(
            "module" to module,
            "variant" to variantName(flavor),
            "versionName" to versionName,
            "versionCode" to versionCode.toString(),
            UUID_KEY to proguardUuid(module, flavor, versionName, versionCode),
        )

    fun writeIdentity(
        dir: File,
        identity: Map<String, String>,
    ) {
        // Plain lines in a fixed order: the same release always writes the same file
        File(dir, IDENTITY_NAME).writeText(identity.entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" })
    }

    // Returns what stops the saved mapping in `dir` from being published as `expected`, or null
    fun identityProblem(
        dir: File,
        expected: Map<String, String>,
    ): String? {
        val identityFile = File(dir, IDENTITY_NAME)
        if (!identityFile.exists()) {
            return "$dir has a mapping without ${IDENTITY_NAME}, so the build it belongs to is unknown"
        }
        val saved = Properties().apply { identityFile.inputStream().use { load(it) } }
        val differences =
            expected
                .filter { saved.getProperty(it.key) != it.value }
                .map { "${it.key} is ${saved.getProperty(it.key)}, expected ${it.value}" }
        return if (differences.isEmpty()) null else "$dir belongs to another build: ${differences.joinToString("; ")}"
    }

    // Flavors (or other directories) below the module directory which hold a saved mapping
    fun savedFlavorDirs(mappingsDir: File): List<String> =
        mappingsDir
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && File(it, ARCHIVE_NAME).exists() }
            .map { it.name }
            .sorted()

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

// A module without flavors has the single release variant, which has no flavor
val releaseFlavors: List<String?> = mappingFlavors.ifEmpty { listOf(null) }
val releaseIdentities: Map<String?, Map<String, String>> =
    releaseFlavors.associateWith { MappingFiles.identity(legacyModuleDirName, it, mappingVersionName, mappingVersionCode) }

// For the module build file: the UUID which the release variant of a flavor carries in its manifest
extra["proguardUuidOfFlavor"] = { flavor: String? ->
    releaseIdentities[flavor]?.getValue(MappingFiles.UUID_KEY)
        ?: throw GradleException("[$legacyModuleDirName] '$flavor' is not in extra['mappingFlavors'] $mappingFlavors")
}

fun String.capitalized(): String = replaceFirstChar { it.uppercase() }

fun saveTaskName(flavor: String?): String = "save${MappingFiles.variantName(flavor).capitalized()}MappingFiles"

// One task per release variant saves the ProGuard mapping, its configuration, the output metadata
// and the identity of the build to mappings/<module>[/<flavor>]
releaseFlavors.forEach { flavor ->
    val variant = MappingFiles.variantName(flavor)
    tasks.register(saveTaskName(flavor)) {
        group = "bisq"
        description = "Save the ProGuard mapping files of $variant to mappings/$legacyModuleDirName"
        // The mapping is always the one of the current sources and version: R8 runs first. So the
        // identity written below is true also when this task is called on its own.
        dependsOn("minify${variant.capitalized()}WithR8")

        // Configuration cache compatible - capture values at configuration time
        val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
        val outputsDirProvider = layout.buildDirectory.dir("outputs")
        val moduleNameForLogging = legacyModuleDirName
        val identity = releaseIdentities.getValue(flavor)

        doLast {
            val buildOutputsDir = outputsDirProvider.get().asFile
            val variantDir = File(buildOutputsDir, "mapping/$variant")
            val targetDir = MappingFiles.flavorDir(File(mappingsDirPath), flavor)
            targetDir.mkdirs()
            // Nothing of an earlier build may stay next to the new mapping
            MappingFiles.savedFileNames.forEach { File(targetDir, it).delete() }

            // Compress and copy mapping.txt as tar.gz
            val mappingFile = File(variantDir, "mapping.txt")
            if (!mappingFile.exists()) {
                throw GradleException("[$moduleNameForLogging] mapping.txt not found at ${mappingFile.absolutePath}")
            }
            val tarGzFile = File(targetDir, MappingFiles.ARCHIVE_NAME)
            val (exitCode, output) =
                MappingFiles.runCommand(listOf("tar", "-czf", tarGzFile.absolutePath, "-C", mappingFile.parent, mappingFile.name))
            if (exitCode != 0) {
                throw GradleException("[$moduleNameForLogging] Failed to create ${MappingFiles.ARCHIVE_NAME}: $output")
            }
            println("[$moduleNameForLogging] Saved ${MappingFiles.ARCHIVE_NAME} to $targetDir")

            // Copy configuration.txt
            val configFile = File(variantDir, MappingFiles.CONFIGURATION_NAME)
            if (configFile.exists()) {
                configFile.copyTo(File(targetDir, MappingFiles.CONFIGURATION_NAME), overwrite = true)
                println("[$moduleNameForLogging] Saved ${MappingFiles.CONFIGURATION_NAME} to $targetDir")
            } else {
                println("[$moduleNameForLogging] ${MappingFiles.CONFIGURATION_NAME} not found at ${configFile.absolutePath}")
            }

            // Copy output-metadata.json from either APK or Bundle output location
            val apkDir = if (flavor == null) "apk/release" else "apk/$flavor/release"
            val apkMetadata = File(buildOutputsDir, "$apkDir/${MappingFiles.OUTPUT_METADATA_NAME}")
            val bundleMetadata = File(buildOutputsDir, "bundle/$variant/${MappingFiles.OUTPUT_METADATA_NAME}")
            val metadata = listOf(apkMetadata, bundleMetadata).firstOrNull { it.exists() }
            if (metadata != null) {
                metadata.copyTo(File(targetDir, MappingFiles.OUTPUT_METADATA_NAME), overwrite = true)
                println("[$moduleNameForLogging] Saved ${MappingFiles.OUTPUT_METADATA_NAME} to $targetDir")
            } else {
                println(
                    "[$moduleNameForLogging] ${MappingFiles.OUTPUT_METADATA_NAME} not found at ${apkMetadata.absolutePath} " +
                        "or ${bundleMetadata.absolutePath}",
                )
            }

            MappingFiles.writeIdentity(targetDir, identity)
            println("[$moduleNameForLogging] Saved ${MappingFiles.IDENTITY_NAME} (${identity.getValue(MappingFiles.UUID_KEY)}) to $targetDir")
        }
    }
}

// Task to save the ProGuard mapping files of all release variants
tasks.register("saveMappingFiles") {
    group = "bisq"
    description = "Save ProGuard mapping files to mappings/$legacyModuleDirName directory"
    dependsOn(releaseFlavors.map { saveTaskName(it) })
}

// Task to upload mapping files to a GitHub release as assets.
// Usage: ./gradlew :apps:clientApp:uploadMappings -PreleaseTag=connect_0.3.0
//        ./gradlew :apps:nodeApp:uploadMappings -PreleaseTag=anode_0.5.0
// Uploads the mappings of the release variants which were built and saved. Every one of them must
// belong to the current version; the task fails otherwise and uploads nothing.
tasks.register("uploadMappings") {
    group = "bisq"
    description = "Upload mapping files from mappings/$legacyModuleDirName to a GitHub release"
    // Never zip files which the save tasks are still writing
    mustRunAfter(releaseFlavors.map { saveTaskName(it) })

    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val moduleNameForLogging = legacyModuleDirName
    val repo = "bisq-network/bisq-mobile"
    val identities = releaseIdentities
    // Capture at configuration time for configuration cache compatibility
    val releaseTag = providers.gradleProperty("releaseTag")

    doLast {
        val tag =
            releaseTag.orNull
                ?: throw GradleException("Required property 'releaseTag' not set. Usage: -PreleaseTag=connect_0.3.0")

        val mappingsDir = File(mappingsDirPath)
        val problems = mutableListOf<String>()
        val isFlavored = identities.keys.none { it == null }
        if (isFlavored) {
            // Files directly in the module directory come from a build before the module had flavors
            MappingFiles.savedFileNames.map { File(mappingsDir, it) }.filter { it.exists() }.forEach {
                problems += "$it does not belong to a flavor of this module"
            }
            (MappingFiles.savedFlavorDirs(mappingsDir) - identities.keys.filterNotNull()).forEach {
                problems += "${File(mappingsDir, it)} is not a flavor of this module"
            }
        }
        val savedDirs =
            identities
                .mapKeys { MappingFiles.flavorDir(mappingsDir, it.key) }
                .filter { File(it.key, MappingFiles.ARCHIVE_NAME).exists() }
        savedDirs.forEach { (dir, expected) -> MappingFiles.identityProblem(dir, expected)?.let { problems += it } }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "[$moduleNameForLogging] Refusing to upload mappings which are not of the current release build. " +
                    "Remove them or run the release build again:\n" + problems.joinToString("\n") { " - $it" },
            )
        }
        if (savedDirs.isEmpty()) {
            throw GradleException("[$moduleNameForLogging] No mapping files found in $mappingsDir. Run a release build first.")
        }

        val zipName = "$moduleNameForLogging-mappings.zip"
        val zipFile = File(mappingsDir, zipName)
        // A zip of an earlier run must not be extended
        zipFile.delete()

        // Paths relative to the mappings directory keep the flavor directories apart in the zip
        val filesToZip =
            savedDirs.keys.flatMap { dir -> MappingFiles.savedFileNames.map { File(dir, it) }.filter { it.exists() } }
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

// Hook after the release build of every variant; works even if the tasks are created later.
// The aggregate assembleRelease and bundleRelease of a module with flavors run the tasks of the flavors.
releaseFlavors.forEach { flavor ->
    val variant = MappingFiles.variantName(flavor).capitalized()
    val releaseBuildTaskNames = setOf("assemble$variant", "bundle$variant")
    tasks.matching { it.name in releaseBuildTaskNames }.configureEach {
        finalizedBy(saveTaskName(flavor))
    }
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
//   ./gradlew :apps:nodeApp:saveMappingFiles  # produces mapping.txt.tar.gz
//   ./gradlew :apps:nodeApp:uploadMappingsToGlitchTip -PreleaseTag=anode_0.5.0
//   A module with flavors needs the flavor of the released build:
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
    mustRunAfter(releaseFlavors.map { saveTaskName(it) })

    val mappingsDirPath = "${rootProject.projectDir}/mappings/$legacyModuleDirName"
    val sentryPropertiesPath = "${project.projectDir}/sentry.properties"
    val moduleNameForLogging = legacyModuleDirName
    val identities = releaseIdentities
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

        // Select the mapping. A module with flavors has one per flavor, so the flavor must be
        // named: an archive directly in the module directory would be one of an earlier build.
        val mappingsDir = File(mappingsDirPath)
        val flavors = identities.keys.filterNotNull()
        val requestedFlavor = mappingFlavorProperty.orNull
        val flavor =
            if (flavors.isEmpty()) {
                if (requestedFlavor != null) {
                    throw GradleException("[$moduleNameForLogging] This module has no flavors, remove -PmappingFlavor=$requestedFlavor.")
                }
                null
            } else {
                val available = MappingFiles.savedFlavorDirs(mappingsDir).filter { it in flavors }
                if (requestedFlavor == null || requestedFlavor !in available) {
                    throw GradleException(
                        "[$moduleNameForLogging] -PmappingFlavor=<flavor> must name a flavor with a saved mapping, " +
                            "got '${requestedFlavor ?: ""}'. Saved flavors: ${available.ifEmpty { listOf("none, run a release build first") }}",
                    )
                }
                requestedFlavor
            }
        val savedDir = MappingFiles.flavorDir(mappingsDir, flavor)
        val tarGz = File(savedDir, MappingFiles.ARCHIVE_NAME)
        if (!tarGz.exists()) {
            throw GradleException(
                "[$moduleNameForLogging] $tarGz not found. Run saveMappingFiles first " +
                    "(or assemble/bundleRelease which finalizes on it).",
            )
        }
        val identity = identities.getValue(flavor)
        MappingFiles.identityProblem(savedDir, identity)?.let {
            throw GradleException("[$moduleNameForLogging] Refusing to upload a mapping which is not of the current release build: $it")
        }

        // The saved mapping is gzip-tar. sentry-cli wants the raw mapping.txt — extract to a temp location.
        val tmpDir = uploadDirProvider.get().asFile
        tmpDir.deleteRecursively()
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
        // The mapping is registered under the UUID which the app of this release carries in its
        // manifest. Without --uuid, sentry-cli derives one from the content of the file, which
        // no app knows.
        val proguardUuid = identity.getValue(MappingFiles.UUID_KEY)
        val steps =
            listOf(
                listOf("sentry-cli", "releases", "new", releaseTag),
                listOf("sentry-cli", "upload-proguard", "--uuid", proguardUuid, mappingTxt.absolutePath),
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
        println("[$moduleNameForLogging] Uploaded mapping $proguardUuid for release '$releaseTag' to GlitchTip")
    }
}
