package network.bisq.mobile.data.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the fingerprints we know about so a future code change can't silently re-break
 * detection for any of them. Add new cases here whenever you hit a real-world emulator
 * that wasn't detected — the failing test will fail loudly instead of the symptom
 * surfacing as "127.0.0.1 wasn't rewritten to 10.0.2.2".
 */
class IsFingerprintAnEmulatorTest {
    @Test
    fun `recognizes 16 KB page-size Pixel emulator on Android 17`() {
        // Fingerprint observed in the field on the sdk_gphone16k_arm64 image —
        // the variant that prompted this fix (see commit message).
        assertTrue(
            check(
                fingerprint = "google/sdk_gphone16k_arm64/emu64a16k:17/CE2A.260420.019/15611780:user/dev-keys",
                model = "sdk_gphone16k_arm64",
                product = "sdk_gphone16k_arm64",
                manufacturer = "Google",
                brand = "google",
            ),
        )
    }

    @Test
    fun `recognizes legacy 32-bit Google emulator`() {
        assertTrue(
            check(
                fingerprint = "google/sdk_gphone_x86/generic_x86:11/RSR1.201013.001/6903271:user/release-keys",
                model = "sdk_gphone_x86",
                product = "sdk_gphone_x86",
                manufacturer = "Google",
                brand = "google",
            ),
        )
    }

    @Test
    fun `recognizes 64-bit Google emulator with dev keys`() {
        assertTrue(
            check(
                fingerprint = "google/sdk_gphone64_arm64/emu64a:15/AE3A.240806.043/12960925:userdebug/dev-keys",
                model = "sdk_gphone64_arm64",
                product = "sdk_gphone64_arm64",
                manufacturer = "Google",
                brand = "google",
            ),
        )
    }

    @Test
    fun `recognizes 64-bit Google emulator with release keys`() {
        assertTrue(
            check(
                fingerprint = "google/sdk_gphone64_x86_64/emu64x:13/TE1A.220922.034/9466628:user/release-keys",
                model = "sdk_gphone64_x86_64",
                product = "sdk_gphone64_x86_64",
                manufacturer = "Google",
                brand = "google",
            ),
        )
    }

    @Test
    fun `recognizes Google Play Games emulator`() {
        assertTrue(
            check(
                fingerprint = "google/kiwi_pc/kiwi_emu_x86_64:11/RSR1.230531.001/8001234:user/release-keys",
                model = "HPE device",
                product = "kiwi_pc",
                board = "kiwi",
                manufacturer = "Google",
                brand = "google",
            ),
        )
    }

    @Test
    fun `does not flag real Pixel 9 device as emulator`() {
        assertFalse(
            check(
                fingerprint = "google/tokay/tokay:15/AP4A.250105.002/12701944:user/release-keys",
                model = "Pixel 9",
                product = "tokay",
                manufacturer = "Google",
                brand = "google",
                device = "tokay",
            ),
        )
    }

    @Test
    fun `does not flag a typical Samsung device as emulator`() {
        assertFalse(
            check(
                fingerprint = "samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S928BXXU2AXBA:user/release-keys",
                model = "SM-S928B",
                product = "dm3qxxx",
                manufacturer = "samsung",
                brand = "samsung",
                device = "dm3q",
            ),
        )
    }

    @Test
    fun `recognizes generic AOSP fingerprint prefix`() {
        assertTrue(
            check(
                fingerprint = "generic/sdk/generic:11/AOSP/eng.builder.20210101.000000:eng/test-keys",
                model = "generic",
                product = "sdk",
                manufacturer = "unknown",
                brand = "generic",
                device = "generic",
            ),
        )
    }

    private fun check(
        fingerprint: String,
        model: String = "",
        manufacturer: String = "",
        board: String = "",
        product: String = "",
        brand: String = "",
        device: String = "",
        host: String = "",
    ): Boolean =
        isFingerprintAnEmulator(
            fingerprint = fingerprint,
            model = model,
            manufacturer = manufacturer,
            board = board,
            product = product,
            brand = brand,
            device = device,
            host = host,
        )
}
