plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

// Mirrors the root build's ktlint setup so the plugin sources are held to the same style.
// build-logic is a separate build, so the root ktlintCheck/ktlintFormat tasks delegate here
// (see the root build.gradle.kts).
ktlint {
    version.set(
        libs.versions.ktlint.cli
            .get(),
    )
    verbose.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
}

dependencies {
    // compileOnly: AGP is already on the consuming build's classpath, and a second copy here
    // would load the Variant API through a different classloader.
    compileOnly("com.android.tools.build:gradle-api:${libs.versions.agp.get()}")
}

gradlePlugin {
    plugins {
        create("appArtifacts") {
            id = "network.bisq.mobile.app-artifacts"
            implementationClass = "network.bisq.gradle.AppArtifactsPlugin"
        }
    }
}
