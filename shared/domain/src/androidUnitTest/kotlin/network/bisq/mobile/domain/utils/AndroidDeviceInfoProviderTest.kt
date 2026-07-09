package network.bisq.mobile.domain.utils

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AndroidDeviceInfoProviderTest {
    @Test
    fun `getTotalRamBytes reads a non-negative value from ActivityManager`() {
        val provider = AndroidDeviceInfoProvider(RuntimeEnvironment.getApplication())
        // Exercises the ActivityManager.getMemoryInfo path; the exact value is environment-dependent,
        // the invariant is that it returns a sane (non-negative) byte count without throwing.
        assertTrue(provider.getTotalRamBytes() >= 0L)
    }
}
