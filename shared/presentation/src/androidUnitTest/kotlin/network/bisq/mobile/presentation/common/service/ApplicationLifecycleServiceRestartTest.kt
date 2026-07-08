package network.bisq.mobile.presentation.common.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.TorBootstrapNotReadyException
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
        var throwTorBootstrapNotReadyOnActivate = false

        override suspend fun deactivateServiceFacades() {
            deactivateCalls++
            if (failOnDeactivate) {
                throw RuntimeException("deactivate failed")
            }
        }

        override suspend fun activateServiceFacades() {
            activateCalls++
            if (throwTorBootstrapNotReadyOnActivate) {
                throw TorBootstrapNotReadyException()
            }
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

    @Test
    fun `restartTorBootstrap deactivates then activates service facades`() =
        runTest {
            val service = TrackingLifecycleService()

            val result = service.restartTorBootstrap()

            assertTrue(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap returns false when deactivation fails`() =
        runTest {
            val service =
                TrackingLifecycleService().apply {
                    failOnDeactivate = true
                }

            val result = service.restartTorBootstrap()

            assertFalse(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(0, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap returns false when activation fails`() =
        runTest {
            val service =
                TrackingLifecycleService().apply {
                    failOnActivate = true
                }

            val result = service.restartTorBootstrap()

            assertFalse(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap returns false when app is terminating`() =
        runTest {
            val service =
                object : TrackingLifecycleService() {
                    init {
                        compareAndSetIsTerminating(expect = false, update = true)
                    }
                }

            val result = service.restartTorBootstrap()

            assertFalse(result)
            assertEquals(0, service.deactivateCalls)
            assertEquals(0, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap purges tor dir when purgeTorDir is true`() =
        runTest {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            coEvery { kmpTorService.stopAndPurgeWorkingDir() } returns Unit
            val service = TrackingLifecycleService(kmpTorService = kmpTorService)

            val result = service.restartTorBootstrap(purgeTorDir = true)

            assertTrue(result)
            coVerify(exactly = 1) { kmpTorService.stopAndPurgeWorkingDir() }
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap returns false when Tor bootstrap is not ready`() =
        runTest {
            val service =
                TrackingLifecycleService().apply {
                    throwTorBootstrapNotReadyOnActivate = true
                }

            val result = service.restartTorBootstrap()

            assertFalse(result)
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }
}
