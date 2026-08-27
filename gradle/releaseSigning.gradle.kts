import java.util.Properties

// Shared by :apps:clientApp and :apps:nodeApp. Loads local.properties, resolves
// KEYSTORE_PATH, and fails closed only when this project is packaging a release.
// App scripts still create signingConfigs.release (alias/password keys differ).

val loadedLocalProperties =
    Properties().apply {
        load(file("${rootDir}/local.properties").inputStream())
    }
extra["localProperties"] = loadedLocalProperties

val releaseKeystorePath =
    (loadedLocalProperties["KEYSTORE_PATH"] as? String)?.takeIf { it.isNotBlank() }
val releaseKeystoreFile =
    releaseKeystorePath
        ?.let { file(it) }
        ?.takeIf { it.isFile && it.canRead() }
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

gradle.taskGraph.whenReady {
    val requestedReleasePackaging =
        allTasks.any { task ->
            task.project == project &&
                (
                    task.name == "assembleRelease" ||
                        task.name == "bundleRelease" ||
                        task.name == "packageRelease" ||
                        task.name == "packageReleaseBundle"
                )
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
        @Suppress("UNCHECKED_CAST")
        val companionProps =
            extra["requiredCompanionProps"] as? List<String>
                ?: error("requiredCompanionProps must be set after applying releaseSigning.gradle.kts")
        companionProps.forEach { requiredSigningProp(it) }
    }
    if (releaseKeystoreFile == null) {
        logger.lifecycle(
            "Packaging an unsigned release because -PallowUnsignedRelease=true.",
        )
    }
}
