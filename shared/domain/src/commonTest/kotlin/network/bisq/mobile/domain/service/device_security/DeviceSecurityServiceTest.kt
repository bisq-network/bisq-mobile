package network.bisq.mobile.domain.service.device_security

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.DeviceSecurityBaseline
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceSecurityServiceTest {
    private class FakeProbe(
        var result: DeviceIntegrity = DeviceIntegrity.CLEAN,
        var failure: Throwable? = null,
    ) : DeviceIntegrityProbe {
        var calls = 0

        override fun detect(): DeviceIntegrity {
            calls++
            failure?.let { throw it }
            return result
        }
    }

    @Test
    fun `nothing is known before the first check`() =
        runTest {
            val service = DeviceSecurityService(FakeProbe(), SettingsRepositoryMock(), StandardTestDispatcher(testScheduler))

            assertNull(service.integrity.value)
        }

    @Test
    fun `a clean arrival persists a clean baseline`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val service = DeviceSecurityService(FakeProbe(DeviceIntegrity.CLEAN), settings, StandardTestDispatcher(testScheduler))

            val resolution = service.onDashboardArrival()

            assertFalse(resolution.isPending)
            assertEquals(DeviceSecurityBaseline.CLEAN, settings.data.value.deviceSecurityBaseline)
            assertEquals(DeviceIntegrity.CLEAN, service.integrity.value)
        }

    @Test
    fun `a rooted arrival is pending and persists a rooted baseline`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val service = DeviceSecurityService(FakeProbe(DeviceIntegrity.ROOTED), settings, StandardTestDispatcher(testScheduler))

            val resolution = service.onDashboardArrival()

            assertTrue(resolution.isPending)
            assertEquals(DeviceSecurityBaseline.ROOTED, settings.data.value.deviceSecurityBaseline)
            assertEquals(DeviceIntegrity.ROOTED, service.integrity.value)
        }

    @Test
    fun `dont show again persists the suppression and the next arrival is not pending`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val service = DeviceSecurityService(FakeProbe(DeviceIntegrity.ROOTED), settings, StandardTestDispatcher(testScheduler))
            service.onDashboardArrival()

            service.suppressWarning()
            val next = service.onDashboardArrival()

            assertTrue(settings.data.value.deviceSecurityWarningSuppressed)
            assertFalse(next.isPending)
            assertTrue(next.isRooted)
        }

    @Test
    fun `a clean arrival after dont show again re-arms the warning with the changed wording on a later rooting`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val probe = FakeProbe(DeviceIntegrity.CLEAN)
            val service = DeviceSecurityService(probe, settings, StandardTestDispatcher(testScheduler))
            service.onDashboardArrival()
            probe.result = DeviceIntegrity.ROOTED
            service.onDashboardArrival()
            service.suppressWarning()

            probe.result = DeviceIntegrity.CLEAN
            service.onDashboardArrival()
            probe.result = DeviceIntegrity.ROOTED
            val rerooted = service.onDashboardArrival()

            assertTrue(rerooted.isPending)
            assertEquals(DeviceSecurityWording.CHANGED_SINCE_SETUP, rerooted.wording)
        }

    @Test
    fun `a probe failure counts as clean and never as rooted`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val probe = FakeProbe(failure = IllegalStateException("probe broke"))
            val service = DeviceSecurityService(probe, settings, StandardTestDispatcher(testScheduler))

            val resolution = service.onDashboardArrival()

            assertFalse(resolution.isPending)
            assertFalse(resolution.isRooted)
            assertEquals(DeviceIntegrity.CLEAN, service.integrity.value)
        }

    @Test
    fun `a probe failure does not record a baseline or clear a suppression`() =
        runTest {
            val settings =
                SettingsRepositoryMock(
                    Settings(
                        deviceSecurityBaseline = DeviceSecurityBaseline.ROOTED,
                        deviceSecurityWarningSuppressed = true,
                    ),
                )
            val service =
                DeviceSecurityService(
                    FakeProbe(failure = IllegalStateException("probe broke")),
                    settings,
                    StandardTestDispatcher(testScheduler),
                )

            service.onDashboardArrival()

            assertEquals(DeviceSecurityBaseline.ROOTED, settings.data.value.deviceSecurityBaseline)
            assertTrue(settings.data.value.deviceSecurityWarningSuppressed)
        }

    @Test
    fun `ensureChecked detects once and reuses the result`() =
        runTest {
            val probe = FakeProbe(DeviceIntegrity.ROOTED)
            val service = DeviceSecurityService(probe, SettingsRepositoryMock(), StandardTestDispatcher(testScheduler))

            service.ensureChecked()
            service.ensureChecked()

            assertEquals(1, probe.calls)
            assertEquals(DeviceIntegrity.ROOTED, service.integrity.value)
        }

    @Test
    fun `clearing the suppression lets the warning show again`() =
        runTest {
            val settings =
                SettingsRepositoryMock(
                    Settings(
                        deviceSecurityBaseline = DeviceSecurityBaseline.ROOTED,
                        deviceSecurityWarningSuppressed = true,
                    ),
                )
            val service = DeviceSecurityService(FakeProbe(DeviceIntegrity.ROOTED), settings, StandardTestDispatcher(testScheduler))

            service.clearSuppression()
            val resolution = service.onDashboardArrival()

            assertFalse(settings.data.value.deviceSecurityWarningSuppressed)
            assertTrue(resolution.isPending)
        }
}
