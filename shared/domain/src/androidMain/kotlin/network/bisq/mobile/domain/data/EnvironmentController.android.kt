package network.bisq.mobile.domain.data

import network.bisq.mobile.client.shared.BuildConfig

// on android there is no 100% accurate way to determine, but this is the most comprehesive one
actual fun provideIsSimulator(): Boolean {
    println("Fingerprint ${android.os.Build.FINGERPRINT}")
    return (android.os.Build.MANUFACTURER == "Google" && android.os.Build.BRAND == "google" &&
                    ((android.os.Build.FINGERPRINT.startsWith("google/sdk_gphone_")
                            && android.os.Build.FINGERPRINT.endsWith(":user/release-keys")
                            && android.os.Build.PRODUCT.startsWith("sdk_gphone_")
                            && android.os.Build.MODEL.startsWith("sdk_gphone_"))
                            //alternative
                            || (android.os.Build.FINGERPRINT.startsWith("google/sdk_gphone64_") && (android.os.Build.FINGERPRINT.endsWith(":userdebug/dev-keys")
                            || (android.os.Build.FINGERPRINT.endsWith(":user/release-keys")) && android.os.Build.PRODUCT.startsWith("sdk_gphone64_")
                            && android.os.Build.MODEL.startsWith("sdk_gphone64_")))
                            //Google Play Games emulator https://play.google.com/googleplaygames https://developer.android.com/games/playgames/emulator#other-downloads
                            || (android.os.Build.MODEL == "HPE device" &&
                            android.os.Build.FINGERPRINT.startsWith("google/kiwi_") && android.os.Build.FINGERPRINT.endsWith(":user/release-keys")
                            && android.os.Build.BOARD == "kiwi" && android.os.Build.PRODUCT.startsWith("kiwi_"))
                            )
                    //
                    || android.os.Build.FINGERPRINT.startsWith("generic")
                    || android.os.Build.FINGERPRINT.startsWith("unknown")
                    || android.os.Build.MODEL.contains("google_sdk")
                    || android.os.Build.MODEL.contains("Emulator")
                    || android.os.Build.MODEL.contains("Android SDK built for x86")
                    //bluestacks
                    || "QC_Reference_Phone" == android.os.Build.BOARD && !"Xiaomi".equals(android.os.Build.MANUFACTURER, ignoreCase = true)
                    //bluestacks
                    || android.os.Build.MANUFACTURER.contains("Genymotion")
                    || android.os.Build.HOST.startsWith("Build")
                    //MSI App Player
                    || android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")
                    || android.os.Build.PRODUCT == "google_sdk"
                    // another Android SDK emulator check
                    || System.getProperties()["ro.kernel.qemu"] == "1")
}

actual fun provideApiHost(): String {
    return BuildConfig.WS_ANDROID_HOST.takeIf { it.isNotEmpty() } ?: "10.0.2.2"
}

actual fun provideApiPort(): Int {
    return (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt()
}