package network.bisq.mobile.presentation.common.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.TorBootstrapNotReadyException
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.NoOpAnalyticsService
import network.bisq.mobile.domain.utils.killProcess
import network.bisq.mobile.domain.utils.restartProcess
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationLifecycleServiceRestartTest : PresentationKoinTestBase() {
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

        // When set, activation suspends on it, like the network facade waiting for Tor
        var activationGate: CompletableDeferred<Unit>? = null
        var cancelledActivations = 0

        // Like the node's application service init on IO, which a cancel doesn't interrupt
        var activationIgnoresCancellation = false

        override suspend fun deactivateServiceFacades() {
            deactivateCalls++
            if (failOnDeactivate) {
                throw RuntimeException("deactivate failed")
            }
        }

        override suspend fun activateServiceFacades() {
            activateCalls++
            try {
                if (activationIgnoresCancellation) {
                    withContext(NonCancellable) { activationGate?.await() }
                } else {
                    activationGate?.await()
                }
            } catch (e: CancellationException) {
                cancelledActivations++
                throw e
            }
            if (throwTorBootstrapNotReadyOnActivate) {
                throw TorBootstrapNotReadyException()
            }
            if (failOnActivate) {
                throw RuntimeException("activate failed")
            }
        }
    }

    @Test
    fun `initialize marks the services activated once activation completes`() =
        runTest {
            val service = TrackingLifecycleService()
            assertFalse(service.areServicesActivated)

            service.initialize()
            runCurrent()

            assertTrue(service.areServicesActivated)
        }

    @Test
    fun `a restart that fails to activate leaves the services deactivated`() =
        runTest {
            val service = TrackingLifecycleService()
            service.initialize()
            runCurrent()
            service.throwTorBootstrapNotReadyOnActivate = true

            val result = service.restartAllServices()

            assertFalse(result)
            assertFalse(service.areServicesActivated)
        }

    @Test
    fun `restartAllServices cancels an in-flight initial activation`() =
        runTest {
            val service = TrackingLifecycleService().apply { activationGate = CompletableDeferred() }
            service.initialize()
            runCurrent()
            assertEquals(1, service.activateCalls)
            assertFalse(service.areServicesActivated)
            service.activationGate = null

            val result = service.restartAllServices()

            assertTrue(result)
            assertEquals(1, service.cancelledActivations)
            assertEquals(1, service.deactivateCalls)
            assertEquals(2, service.activateCalls)
            assertTrue(service.areServicesActivated)
        }

    @Test
    fun `terminateApp kills the process without waiting for an in-flight activation`() =
        runTest {
            mockkStatic(PROCESS_UTILS)
            try {
                every { killProcess(any()) } just runs
                val service = blockedInitialActivation()

                service.terminateApp(null)
                runCurrent()

                assertEquals(1, service.deactivateCalls)
                verify(exactly = 1) { killProcess(null) }
            } finally {
                unmockkStatic(PROCESS_UTILS)
            }
        }

    @Test
    fun `restartApp restarts the process without waiting for an in-flight activation`() =
        runTest {
            mockkStatic(PROCESS_UTILS)
            try {
                every { restartProcess(any()) } just runs
                val service = blockedInitialActivation()

                service.restartApp(null, purgeTorDir = false)
                runCurrent()

                assertEquals(1, service.deactivateCalls)
                verify(exactly = 1) { restartProcess(null) }
            } finally {
                unmockkStatic(PROCESS_UTILS)
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

    @Test
    fun `overlapping restarts run one after the other`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val service = TrackingLifecycleService().apply { activationGate = gate }

            val first = async { service.restartAllServices() }
            val second = async { service.restartAllServices() }
            runCurrent()

            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)

            gate.complete(Unit)
            runCurrent()

            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(2, service.deactivateCalls)
            assertEquals(2, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap is ignored while a restart is running`() =
        runTest {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val gate = CompletableDeferred<Unit>()
            val service = TrackingLifecycleService(kmpTorService = kmpTorService).apply { activationGate = gate }
            launch { service.restartAllServices() }
            runCurrent()

            val torRestart = async { service.restartTorBootstrap(purgeTorDir = true) }
            runCurrent()

            assertTrue(torRestart.isCompleted)
            assertFalse(torRestart.await())
            assertEquals(1, service.deactivateCalls)
            coVerify(exactly = 0) { kmpTorService.stopAndPurgeWorkingDir(any()) }
            gate.complete(Unit)
        }

    @Test
    fun `a restart queued while the app starts terminating does not run`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val service = TrackingLifecycleService().apply { activationGate = gate }
            launch { service.restartAllServices() }
            runCurrent()
            val queued = async { service.restartAllServices() }
            runCurrent()

            service.compareAndSetIsTerminating(expect = false, update = true)
            gate.complete(Unit)
            runCurrent()

            assertFalse(queued.await())
            assertEquals(1, service.deactivateCalls)
            assertEquals(1, service.activateCalls)
        }

    @Test
    fun `restartTorBootstrap releases the lock when it fails`() =
        runTest {
            val service = TrackingLifecycleService().apply { failOnActivate = true }
            assertFalse(service.restartTorBootstrap())
            service.failOnActivate = false

            val restart = async { service.restartAllServices() }
            runCurrent()

            assertTrue(restart.isCompleted)
            assertTrue(restart.await())
        }

    private fun TestScope.blockedInitialActivation(): TrackingLifecycleService {
        val service =
            TrackingLifecycleService().apply {
                activationGate = CompletableDeferred()
                activationIgnoresCancellation = true
            }
        service.initialize()
        runCurrent()
        return service
    }

    private companion object {
        const val PROCESS_UTILS = "network.bisq.mobile.domain.utils.ProcessUtils_androidKt"
    }
}
