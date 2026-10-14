package network.bisq.mobile.domain.service.device_security

import network.bisq.mobile.data.model.DeviceSecurityBaseline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceSecurityResolutionTest {
    @Test
    fun `clean on first arrival records a clean baseline and shows nothing`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.CLEAN, DeviceSecurityBaseline.UNSET, suppressed = false)

        assertEquals(DeviceSecurityBaseline.CLEAN, resolution.baseline)
        assertFalse(resolution.suppressed)
        assertFalse(resolution.isPending)
        assertFalse(resolution.isRooted)
        assertNull(resolution.wording)
    }

    @Test
    fun `rooted on first arrival records a rooted baseline and is pending with the standard wording`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.UNSET, suppressed = false)

        assertEquals(DeviceSecurityBaseline.ROOTED, resolution.baseline)
        assertTrue(resolution.isPending)
        assertTrue(resolution.isRooted)
        assertEquals(DeviceSecurityWording.FIRST_DETECTION, resolution.wording)
    }

    @Test
    fun `rooted with a rooted baseline stays pending with the standard wording`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.ROOTED, suppressed = false)

        assertEquals(DeviceSecurityBaseline.ROOTED, resolution.baseline)
        assertTrue(resolution.isPending)
        assertEquals(DeviceSecurityWording.FIRST_DETECTION, resolution.wording)
    }

    @Test
    fun `rooted after a clean baseline is pending with the changed wording`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.CLEAN, suppressed = false)

        assertEquals(DeviceSecurityBaseline.CLEAN, resolution.baseline)
        assertTrue(resolution.isPending)
        assertEquals(DeviceSecurityWording.CHANGED_SINCE_SETUP, resolution.wording)
    }

    @Test
    fun `rooted and suppressed is not pending but still rooted`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.ROOTED, suppressed = true)

        assertTrue(resolution.suppressed)
        assertFalse(resolution.isPending)
        assertTrue(resolution.isRooted)
    }

    @Test
    fun `rooted on first arrival keeps an existing suppression`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.UNSET, suppressed = true)

        assertTrue(resolution.suppressed)
        assertFalse(resolution.isPending)
    }

    @Test
    fun `clean clears a suppression so a later rooting is announced again`() {
        val resolution = resolveDeviceSecurity(DeviceIntegrity.CLEAN, DeviceSecurityBaseline.ROOTED, suppressed = true)

        assertFalse(resolution.suppressed)
        assertFalse(resolution.isPending)
    }

    @Test
    fun `the baseline is never overwritten once recorded`() {
        assertEquals(
            DeviceSecurityBaseline.ROOTED,
            resolveDeviceSecurity(DeviceIntegrity.CLEAN, DeviceSecurityBaseline.ROOTED, suppressed = false).baseline,
        )
        assertEquals(
            DeviceSecurityBaseline.CLEAN,
            resolveDeviceSecurity(DeviceIntegrity.ROOTED, DeviceSecurityBaseline.CLEAN, suppressed = false).baseline,
        )
    }
}
