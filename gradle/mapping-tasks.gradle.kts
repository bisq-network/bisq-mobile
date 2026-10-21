import java.util.Properties
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
 * the one of the current release, so a mapping of another version or variant cannot be published
 * for it. A rebuild with the same version name and code has the same identity and is not detected.
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

// The release name the app reports to GlitchTip (see the analytics config in each app's DI module)
val glitchTipReleasePrefix =
    when (moduleName) {
        "nodeApp" -> "bisq-easy-node"
        "clientApp" -> "bisq-connect"
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

    // A NO_PROXY of the developer's shell (e.g. `*`) would make curl bypass Tor and resolve the onion directly
    val NO_PROXY_BYPASS_CLEARED = mapOf("NO_PROXY" to "", "no_proxy" to "")

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

    // GlitchTip 6 stores a proguard mapping only through the legacy endpoint (POST files/dsyms/, a zip
    // of proguard/<uuid>.txt). `sentry-cli upload-proguard` sends it as chunks to files/difs/assemble/,
    // which GlitchTip reads only as a native object file: it drops the mapping with "unsupported object
    // file format" while sentry-cli reports success.
    fun uploadProguardMapping(
        sentryProperties: File,
        mappingTxt: File,
        proguardUuid: String,
        workDir: File,
        moduleNameForLogging: String,
    ) {
        val properties = Properties().apply { sentryProperties.inputStream().use { load(it) } }
        fun property(key: String): String =
            properties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw GradleException("[$moduleNameForLogging] $key missing in ${sentryProperties.path}")
        val url = property("defaults.url").trimEnd('/')
        val org = property("defaults.org")
        val project = property("defaults.project")

        val zip = File(workDir, "proguard-mapping.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            // No directory entries: the endpoint rejects every entry which is not proguard/<uuid>.txt.
            out.putNextEntry(ZipEntry("proguard/$proguardUuid.txt"))
            mappingTxt.inputStream().use { it.copyTo(out) }
            out.closeEntry()
        }

        // The token goes to curl on stdin (-H @-), so that it is neither in the process list nor on disk.
        val authHeader = "Authorization: Bearer ${property("auth.token")}\n"
        try {
            val (exit, output) =
                MappingFiles.runCommand(
                    listOf(
                        "curl",
                        "--silent",
                        "--show-error",
                        "--fail-with-body",
                        "--max-time",
                        "600",
                        "--socks5-hostname",
                        "127.0.0.1:9050",
                        "-H",
                        "@-",
                        "-F",
                        "file=@${zip.absolutePath}",
                        "$url/api/0/projects/$org/$project/files/dsyms/",
                    ),
                    environment = NO_PROXY_BYPASS_CLEARED,
                    stdin = authHeader,
                )
            if (exit != 0 || !output.contains(proguardUuid)) {
                throw GradleException("[$moduleNameForLogging] Proguard mapping upload failed (curl exit $exit): $output")
            }
            println("[$moduleNameForLogging] Stored proguard mapping $proguardUuid in GlitchTip project $project")
        } finally {
            zip.delete()
        }
    }

    fun runCommand(
        command: List<String>,
        workingDir: File? = null,
        environment: Map<String, String> = emptyMap(),
        stdin: String? = null,
    ): Pair<Int, String> {
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        workingDir?.let { builder.directory(it) }
        builder.environment().putAll(environment)
        val process = builder.start()
        // Closing stdin also when nothing is passed, so a command never waits for input
        process.outputStream.bufferedWriter().use { writer -> stdin?.let { writer.write(it) } }
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
// is a Tor onion only). No other task depends on it; it runs only when invoked,
// and then fails when sentry.properties or sentry-cli is missing.
//
// The mapping is registered under the release name the app reports
// (bisq-connect@<versionName> / bisq-easy-node@<versionName>), taken from the
// identity of the saved mapping.
//
// Usage (from a checkout of the release tag):
//   ./gradlew :apps:nodeApp:saveMappingFiles  # produces mapping.txt.tar.gz
//   ./gradlew :apps:nodeApp:uploadMappingsToGlitchTip
//   A module with flavors needs the flavor of the released build:
//   ./gradlew :apps:clientApp:uploadMappingsToGlitchTip -PmappingFlavor=google
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
    val releasePrefix = glitchTipReleasePrefix
    val mappingFlavorProperty = providers.gradleProperty("mappingFlavor")
    val uploadDirProvider = layout.buildDirectory.dir("glitchtip-upload")

    doLast {
        val sentryProperties = File(sentryPropertiesPath)
        if (!sentryProperties.exists()) {
            throw GradleException(
                "[$moduleNameForLogging] $sentryPropertiesPath not present. " +
                    "Copy sentry.properties.template, fill in auth.token, then re-run.",
            )
        }

        // Check sentry-cli on PATH, for a helpful message rather than a stack trace from ProcessBuilder.
        val (whichExit, _) = MappingFiles.runCommand(listOf("sh", "-c", "command -v sentry-cli"))
        if (whichExit != 0) {
            throw GradleException(
                "[$moduleNameForLogging] sentry-cli not on PATH. " +
                    "Install with: brew install getsentry/tools/sentry-cli",
            )
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

        // Invoke sentry-cli through Tor; socks5h:// makes DNS happen via Tor too, which is
        // required for .onion resolution. sentry-cli 3 uses curl, which reads only the
        // lowercase http_proxy for http:// URLs (the GlitchTip onion URL is http://).
        // The uppercase names are kept for older sentry-cli versions.
        val env =
            mapOf(
                "http_proxy" to "socks5h://127.0.0.1:9050",
                "https_proxy" to "socks5h://127.0.0.1:9050",
                "HTTPS_PROXY" to "socks5h://127.0.0.1:9050",
                "HTTP_PROXY" to "socks5h://127.0.0.1:9050",
                "SENTRY_PROPERTIES" to sentryProperties.absolutePath,
            ) + MappingFiles.NO_PROXY_BYPASS_CLEARED

        // The mapping is registered under the UUID which the app of this release carries in its
        // manifest.
        val proguardUuid = identity.getValue(MappingFiles.UUID_KEY)
        val releaseName = "$releasePrefix@${identity.getValue("versionName")}"

        fun sentryCli(vararg args: String) {
            val cmd = listOf("sentry-cli", *args)
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

        // Create release (idempotent), upload proguard mapping, finalize.
        sentryCli("releases", "new", releaseName)
        MappingFiles.uploadProguardMapping(sentryProperties, mappingTxt, proguardUuid, tmpDir, moduleNameForLogging)
        sentryCli("releases", "finalize", releaseName)
        println("[$moduleNameForLogging] Uploaded mapping $proguardUuid for release '$releaseName' to GlitchTip")
    }
}
