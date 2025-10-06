plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.cocoapods).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.buildconfig).apply(false)
    alias(libs.plugins.protobuf).apply(false)


    // For Java & KotlinMultiplatform/Jvm this is for stripping out unused compilations
    // of tor to reduce application binary size by keeping only the host/architecture
    // necessary for that distribution.
    // See: https://github.com/05nelsonm/kmp-tor-resource/blob/master/library/resource-filterjar-gradle-plugin/README.md
    alias(libs.plugins.kmp.tor.resource.filterjar).apply(false)

    // For iOS device some setup is needed to incorporate the LibTor.framework
    // that is expected to be present at runtime.
    // See: https://github.com/05nelsonm/kmp-tor-resource/blob/master/library/resource-frameworks-gradle-plugin/README.md
    alias(libs.plugins.kmp.tor.resource.frameworks).apply(false)
}

// Configure all subprojects to run generateResourceBundles before compilation
subprojects {
    afterEvaluate {
        // Only apply to projects that have the generateResourceBundles task
        tasks.findByName("generateResourceBundles")?.let { generateTask ->
            // Make all compile-related tasks depend on generateResourceBundles
            tasks.matching { task ->
                task.name.contains("compile", ignoreCase = true) ||
                task.name.contains("build", ignoreCase = true) ||
                task.name.startsWith("assemble") ||
                task.name.startsWith("bundle")
            }.configureEach {
                dependsOn(generateTask)
            }
        }
    }
}

// ios versioning linking
tasks.register("updatePlist") {
    doLast {
        val plistFile = file("iosClient/iosClient/Info.plist") // Adjust path if needed
        if (!plistFile.exists()) {
            throw GradleException("Info.plist not found at ${plistFile.absolutePath}")
        }

        // Version code should be updated manually on release
        val version = project.findProperty("client.ios.version") as String
        val versionCode = project.findProperty("client.ios.version.code") as String

        val plistContent = plistFile.readText()
            .replace("<key>CFBundleShortVersionString</key>\\s*<string>.*?</string>".toRegex(),
                "<key>CFBundleShortVersionString</key>\n\t<string>$version</string>")
            .replace("<key>CFBundleVersion</key>\\s*<string>.*?</string>".toRegex(),
                "<key>CFBundleVersion</key>\n\t<string>$versionCode</string>")

        plistFile.writeText(plistContent)
        println("Updated Info.plist with version: $version")
    }
}

// Ensure it runs before iOS builds
tasks.matching { it.name.startsWith("link") }.configureEach {
    dependsOn("updatePlist")
}