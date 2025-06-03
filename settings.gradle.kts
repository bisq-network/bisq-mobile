enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // always, for faster builds
        mavenLocal()
        
        // Check if we're running in CI environment
        val isCi = System.getenv("CI") == "true"
        if (isCi) {
            // Use the remote Maven repository for CI builds
            maven {
                url = uri("http://104.154.164.188");
                isAllowInsecureProtocol = true
                credentials {
                    username = "bisq-ci"
                    password = System.getenv("MAVEN_PASSWORD") ?: "bisq-mobile-ci-21!"
                }
            }
        }

        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "BisqApps"
include(":shared:domain")
include(":shared:presentation")
include(":androidClient")
include(":androidNode")