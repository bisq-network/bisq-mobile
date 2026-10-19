import java.util.Properties

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// The node app resolves bisq2 from JitPack. BISQ2_FROM_MAVEN_LOCAL=true in local.properties
// switches it to the jars a local `./gradlew publishAll` of bisq2 installs into ~/.m2; the node
// app's build script swaps the coordinates. See README "Getting started for Android Node".
val bisq2FromMavenLocal =
    Properties()
        .apply { file("local.properties").takeIf { it.isFile }?.reader()?.use { load(it) } }
        .getProperty("BISQ2_FROM_MAVEN_LOCAL")
        .toBoolean()
gradle.extra["bisq2FromMavenLocal"] = bisq2FromMavenLocal

// The groups a local publishAll of bisq2 installs its jars under. No checksum covers them, so only
// ~/.m2 may serve them, and only when that switch is set: no other repository can put bisq2 code
// into a build.
val bisq2LocalGroups = listOf("bisq", "network", "tor")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content { bisq2LocalGroups.forEach { excludeGroup(it) } }
        }
        mavenCentral {
            content { bisq2LocalGroups.forEach { excludeGroup(it) } }
        }

        if (bisq2FromMavenLocal) {
            // Only bisq2's groups: ~/.m2 holds whatever was last installed there, so it must not
            // shadow any other dependency.
            exclusiveContent {
                forRepository { mavenLocal() }
                filter { bisq2LocalGroups.forEach { includeGroup(it) } }
            }
        }

        // JitPack builds bisq2 from source; nothing else is needed from it, and no other
        // repository may serve bisq2.
        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("com.github.bisq-network.bisq2") }
        }
    }
}

rootProject.name = "BisqApps"
include(":shared:domain")
include(":shared:presentation")
include(":shared:kscan")
include(":shared:test-utils")
include(":apps:clientApp")
include(":apps:nodeApp")
