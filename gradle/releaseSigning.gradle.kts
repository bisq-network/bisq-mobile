import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.util.Properties

// Shared by :apps:clientApp and :apps:nodeApp. Loads local.properties, resolves
// KEYSTORE_PATH, and fails closed only when this project is packaging a release.
// App scripts still create signingConfigs.release (alias/password keys differ).

// ValueSource so chmod / format changes invalidate the configuration cache.
// Regular File.canRead() during configuration is snapshotted and not rechecked on reuse.
abstract class KeystoreFileState : ValueSource<String, KeystoreFileState.Params> {
    interface Params : ValueSourceParameters {
        val path: Property<String>
    }

    override fun obtain(): String {
        val raw = parameters.path.orNull.orEmpty()
        if (raw.isBlank()) return "unset"
        val f = File(raw)
        if (!f.isFile) return "missing"
        if (!f.canRead()) return "unreadable"
        val isKeystore =
            f.inputStream().use { ins ->
                val header = ByteArray(4)
                if (ins.read(header) < 4) return@use false
                val b0 = header[0].toInt() and 0xff
                val b1 = header[1].toInt() and 0xff
                val b2 = header[2].toInt() and 0xff
                val b3 = header[3].toInt() and 0xff
                val jks = b0 == 0xfe && b1 == 0xed && b2 == 0xfe && b3 == 0xed
                val jceks = b0 == 0xce && b1 == 0xce && b2 == 0xce && b3 == 0xce
                val pkcs12 = b0 == 0x30
                jks || jceks || pkcs12
            }
        return if (isKeystore) "keystore" else "not-keystore"
    }
}

val loadedLocalProperties =
    Properties().apply {
        load(file("${rootDir}/local.properties").inputStream())
    }
extra["localProperties"] = loadedLocalProperties

val releaseKeystorePath =
    (loadedLocalProperties["KEYSTORE_PATH"] as? String)?.takeIf { it.isNotBlank() }

val keystoreFileState =
    providers
        .of(KeystoreFileState::class.java) {
            parameters.path.set(releaseKeystorePath ?: "")
        }.get()

val releaseKeystoreFile =
    releaseKeystorePath
        ?.takeIf { keystoreFileState == "keystore" }
        ?.let { file(it) }
extra["releaseKeystoreFile"] = releaseKeystoreFile

val allowUnsignedRelease =
    providers
        .gradleProperty("allowUnsignedRelease")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)
        .get()

val requiredSigningProp: (String) -> String = { name ->
    checkNotNull((loadedLocalProperties[name] as? String)?.takeIf { it.isNotBlank() }) {
        "$name must be set in local.properties when KEYSTORE_PATH is set."
    }
}

// Non-throwing lookup for signingConfigs.release so configuration / debug / IDE
// sync do not fail. Named errors still run from whenReady on release packaging.
extra["optionalSigningProp"] = { name: String ->
    (loadedLocalProperties[name] as? String)?.takeIf { it.isNotBlank() } ?: ""
}

fun companionPropsOrError(): List<String> {
    @Suppress("UNCHECKED_CAST")
    return extra["requiredCompanionProps"] as? List<String>
        ?: error("requiredCompanionProps must be set after applying releaseSigning.gradle.kts")
}

gradle.taskGraph.whenReady {
    val thisProject = project
    val requestedReleasePackaging =
        allTasks.any { task ->
            task.project == thisProject &&
                (
                    task.name == "assembleRelease" ||
                        task.name == "bundleRelease" ||
                        task.name == "packageRelease" ||
                        task.name == "packageReleaseBundle"
                )
        }
    val requestedProfilePackaging =
        allTasks.any { task ->
            task.project == thisProject &&
                (task.name == "assembleProfile" || task.name == "packageProfile")
        }

    // Profile falls back to debug when there is no usable release keystore.
    // A readable keystore still requires this app's companions (named error).
    if (requestedProfilePackaging && releaseKeystoreFile != null) {
        companionPropsOrError().forEach { requiredSigningProp(it) }
    }

    if (!requestedReleasePackaging) return@whenReady
    check(releaseKeystorePath == null || releaseKeystoreFile != null) {
        "KEYSTORE_PATH is set to '$releaseKeystorePath' but that path is not a readable keystore file. " +
            "Fix the path or remove KEYSTORE_PATH."
    }
    check(releaseKeystoreFile != null || allowUnsignedRelease) {
        "Release packaging needs a readable KEYSTORE_PATH, or pass " +
            "-PallowUnsignedRelease=true for an unsigned APK."
    }
    if (releaseKeystoreFile != null) {
        companionPropsOrError().forEach { requiredSigningProp(it) }
    }
    if (releaseKeystoreFile == null) {
        logger.lifecycle(
            "Packaging an unsigned release because -PallowUnsignedRelease=true.",
        )
    }
}
