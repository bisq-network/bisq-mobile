plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
//    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)

    alias(libs.plugins.buildconfig).apply(false)

    alias(libs.plugins.protobuf).apply(false)
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