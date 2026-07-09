package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceInfoProviderTest {
    private fun providerWithRam(bytes: Long): DeviceInfoProvider =
        object : DeviceInfoProvider {
            override fun getDeviceInfo(): String = ""

            override fun getTotalRamBytes(): Long = bytes
        }

    @Test
    fun `nominal 3GB device reports low-spec`() {
        // A "3GB" device reports ~2.7-2.9 GiB total (kernel reserve).
        assertTrue(providerWithRam(2_900_000_000L).isLowSpecDevice())
    }

    @Test
    fun `nominal 4GB device is not low-spec`() {
        // A "4GB" device reports ~3.6-3.8 GiB, above the ~3.2 GiB threshold.
        assertFalse(providerWithRam(3_972_844_748L).isLowSpecDevice())
    }

    @Test
    fun `at the threshold is not low-spec`() {
        assertFalse(providerWithRam(DeviceInfoProvider.LOW_SPEC_RAM_THRESHOLD_BYTES).isLowSpecDevice())
    }

    @Test
    fun `just below the threshold is low-spec`() {
        assertTrue(providerWithRam(DeviceInfoProvider.LOW_SPEC_RAM_THRESHOLD_BYTES - 1).isLowSpecDevice())
    }

    @Test
    fun `unknown ram of zero is not low-spec`() {
        // We don't disable animations on a device we couldn't measure.
        assertFalse(providerWithRam(0L).isLowSpecDevice())
    }
}
