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
        // for androidNode till we get bisq-core libs published to a public repo
        mavenLocal()
        maven {
            url = uri("https://jitpack.io")
        }
    }
    repositories {
    }
}

rootProject.name = "BisqApps"
include(":shared:domain")
include(":shared:presentation")
include(":androidClient")
include(":androidNode")