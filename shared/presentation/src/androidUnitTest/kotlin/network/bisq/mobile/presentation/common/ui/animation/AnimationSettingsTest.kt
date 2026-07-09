package network.bisq.mobile.presentation.common.ui.animation

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationSettingsTest {
    private fun settings(useAnimations: Boolean): SettingsServiceFacade =
        mockk(relaxed = true) {
            every { this@mockk.useAnimations } returns MutableStateFlow(useAnimations)
        }

    private fun device(ramBytes: Long): DeviceInfoProvider =
        object : DeviceInfoProvider {
            override fun getDeviceInfo(): String = ""

            override fun getTotalRamBytes(): Long = ramBytes
        }

    // 3GB device reports ~2.8 GiB; 4GB device reports ~3.7 GiB.
    private val lowSpecRam = 2_900_000_000L
    private val capableRam = 3_972_844_748L

    @Test
    fun `no device lock - enabled follows the user setting (Connect behaviour)`() {
        val onCapable = AnimationSettings(settings(true), device(lowSpecRam), applyDeviceLock = false)
        assertFalse(onCapable.lockedByDevice)
        assertTrue(onCapable.enabled.value)

        val off = AnimationSettings(settings(false), device(lowSpecRam), applyDeviceLock = false)
        assertFalse(off.lockedByDevice)
        assertFalse(off.enabled.value)
    }

    @Test
    fun `device lock on but capable device - not locked, follows setting`() {
        val subject = AnimationSettings(settings(true), device(capableRam), applyDeviceLock = true)
        assertFalse(subject.lockedByDevice)
        assertTrue(subject.enabled.value)
    }

    @Test
    fun `device lock on and low-spec device - forced off even when user setting is on`() {
        val subject = AnimationSettings(settings(true), device(lowSpecRam), applyDeviceLock = true)
        assertTrue(subject.lockedByDevice)
        assertFalse(subject.enabled.value)
    }
}
