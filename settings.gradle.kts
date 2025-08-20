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
        val mavenUrl = System.getenv("MAVEN_URL")
        if (isCi && mavenUrl != null) {
            // Use the remote Maven repository for CI builds
            maven {
                url = uri(mavenUrl)
                isAllowInsecureProtocol = true
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
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