package network.bisq.mobile.data.utils

import android.os.Build
import network.bisq.mobile.client.shared.BuildConfig

/**
 * On Android there is no 100% accurate way to detect emulators, but [Build.FINGERPRINT]
 * is the most reliable signal. We delegate to [isFingerprintAnEmulator] so the logic is
 * unit-testable independently of [Build].
 */
actual fun provideIsSimulator(): Boolean {
    println("Fingerprint ${Build.FINGERPRINT}")
    return isFingerprintAnEmulator(
        fingerprint = Build.FINGERPRINT,
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        board = Build.BOARD,
        product = Build.PRODUCT,
        brand = Build.BRAND,
        device = Build.DEVICE,
        host = Build.HOST,
    )
}

/**
 * Liberal emulator detection by signal aggregation. We previously enumerated every
 * known Google emulator fingerprint prefix/suffix combination (`sdk_gphone_`,
 * `sdk_gphone64_`, …) which broke whenever Google introduced a new variant — most
 * recently the 16 KB-page-size emulator `sdk_gphone16k_arm64` with `:user/dev-keys`,
 * which the old check missed and silently disabled the `127.0.0.1 → 10.0.2.2`
 * pairing URL rewrite for emulator users.
 *
 * The single liberal `contains("/sdk_gphone")` check below catches all current and
 * future Google emulator variants — `sdk_gphone` is a Google emulator naming
 * convention, not used by real-device manufacturers, so false-positive risk is
 * negligible.
 */
internal fun isFingerprintAnEmulator(
    fingerprint: String,
    model: String,
    manufacturer: String,
    board: String,
    product: String,
    brand: String,
    device: String,
    host: String,
): Boolean =
    // Google emulators across all known variants (any sdk_gphone* product naming):
    // sdk_gphone_, sdk_gphone64_, sdk_gphone16k_arm64, future variants.
    fingerprint.contains("/sdk_gphone") ||
        // Google Play Games emulator: https://developer.android.com/games/playgames/emulator
        (
            model == "HPE device" &&
                fingerprint.startsWith("google/kiwi_") &&
                fingerprint.endsWith(":user/release-keys") &&
                board == "kiwi" &&
                product.startsWith("kiwi_")
        ) ||
        // Generic AOSP/emulator builds
        fingerprint.startsWith("generic") ||
        fingerprint.startsWith("unknown") ||
        model.contains("google_sdk") ||
        model.contains("Emulator") ||
        model.contains("Android SDK built for x86") ||
        // BlueStacks
        ("QC_Reference_Phone" == board && !"Xiaomi".equals(manufacturer, ignoreCase = true)) ||
        // Genymotion
        manufacturer.contains("Genymotion") ||
        host.startsWith("Build") ||
        // MSI App Player
        (brand.startsWith("generic") && device.startsWith("generic")) ||
        product == "google_sdk" ||
        // QEMU host indicator (still useful for some images)
        System.getProperties()["ro.kernel.qemu"] == "1"

actual fun provideApiHost(): String = BuildConfig.WS_ANDROID_HOST.takeIf { it.isNotEmpty() } ?: "10.0.2.2"

actual fun provideApiPort(): Int = (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt()
