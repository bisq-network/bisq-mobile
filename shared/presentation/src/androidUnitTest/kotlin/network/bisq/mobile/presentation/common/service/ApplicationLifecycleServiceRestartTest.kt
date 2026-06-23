package network.bisq.mobile.presentation.common.service

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.NoOpAnalyticsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationLifecycleServiceRestartTest {
    private open class TrackingLifecycleService(
        applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true),
        kmpTorService: KmpTorService = mockk(relaxed = true),
    ) : ApplicationLifecycleService(
            applicationBootstrapFacade,
            kmpTorService,
            NoOpAnalyticsService,
            AnalyticsBootstrapConfig(dsn = "", environment = "test", release = "test", isDebug = false),
        ) {
        var deactivateCalls = 0
        var activateCalls = 0
        var failOnDeactivate = false
        var failOnActivate = false

        override suspend fun deactivateServiceFacades() {
            deactivateCalls++
            if (failOnDeactivate) {
                throw RuntimeException("deactivate failed")
            }
        }

        override suspend fun activateServiceFacades() {
            activateCalls++
            if (failOnActivate) {
                throw RuntimeException("activate failed")
            }
        }
    }

    @Test
    fun `restartAllServices deactivates then activates service facades`() =
        runTest {
            val service = TrackingLifecycleService()

            val result = service.restartAllServices()

            assertTrue(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartAllServices returns false when deactivation fails`() =
        runTest {
            val service =
                TrackingLifecycleService().apply {
                    failOnDeactivate = true
                }

            val result = service.restartAllServices()

            assertFalse(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(0, service.activateCalls)
        }

    @Test
    fun `restartAllServices returns false when activation fails`() =
        runTest {
            val service =
                TrackingLifecycleService().apply {
                    failOnActivate = true
                }

            val result = service.restartAllServices()

            assertFalse(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartAllServices returns false when app is terminating`() =
        runTest {
            val service =
                object : TrackingLifecycleService() {
                    init {
                        compareAndSetIsTerminating(expect = false, update = true)
                    }
                }

            val result = service.restartAllServices()

            assertFalse(result)
        }
}
