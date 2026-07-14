package network.bisq.mobile.domain.service.network

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.di.testModule
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.network.TorBootstrapNotReadyException
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkServiceFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var kmpTorService: KmpTorService
    private lateinit var applicationBootstrapFacade: ApplicationBootstrapFacade
    private lateinit var torStateFlow: MutableStateFlow<KmpTorService.TorState>
    private lateinit var torBootstrapFailedFlow: MutableStateFlow<Boolean>

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        kmpTorService = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)
        torStateFlow = MutableStateFlow(KmpTorService.TorState.Stopped())
        torBootstrapFailedFlow = MutableStateFlow(false)
        every { kmpTorService.state } returns torStateFlow
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
        every { applicationBootstrapFacade.torBootstrapFailed } returns torBootstrapFailedFlow
        coEvery { kmpTorService.startTor(any(), any()) } returns false
        coEvery { kmpTorService.stopTor(any()) } returns Unit
        startKoin { modules(testModule) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createFacade(torEnabled: Boolean): TestNetworkServiceFacade = TestNetworkServiceFacade(kmpTorService, applicationBootstrapFacade, torEnabled)

    @Test
    fun `ensureTorRunning starts tor when enabled and stopped`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            facade.ensureTorRunning()
            coVerify(exactly = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning does nothing when tor disabled`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = false)
            facade.ensureTorRunning()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning does nothing when tor already started`() =
        runTest(testDispatcher) {
            torStateFlow.value = KmpTorService.TorState.Started
            val facade = createFacade(torEnabled = true)
            facade.ensureTorRunning()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning catches non-cancellation exception and returns`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } throws RuntimeException("tor error")
            facade.ensureTorRunning()
        }

    @Test
    fun `activate does not start tor when disabled`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = false)
            facade.activate()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate starts tor once when it succeeds first attempt`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } coAnswers {
                torStateFlow.value = KmpTorService.TorState.Started
                true
            }
            facade.activate()
            coVerify(exactly = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate retries after a failed start and stops once tor is up`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            var attempt = 0
            coEvery { kmpTorService.startTor(any(), any()) } coAnswers {
                attempt++
                if (attempt == 1) {
                    false
                } else {
                    torStateFlow.value = KmpTorService.TorState.Started
                    true
                }
            }
            facade.activate()
            coVerify(exactly = 2) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `startTorWithRetries stops when terminal bootstrap failure is flagged`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } coAnswers {
                torBootstrapFailedFlow.value = true
                false
            }
            val activateJob =
                launch {
                    assertFailsWith<TorBootstrapNotReadyException> {
                        facade.activate()
                    }
                }
            runCurrent()
            activateJob.join()
            coVerify(exactly = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate throws TorBootstrapNotReadyException after max start attempts and bootstrap failure`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } returns false
            val activateJob =
                launch {
                    assertFailsWith<TorBootstrapNotReadyException> {
                        facade.activate()
                    }
                }
            runCurrent()
            advanceTimeBy(2_000L)
            runCurrent()
            torBootstrapFailedFlow.value = true
            runCurrent()
            activateJob.join()
            coVerify(exactly = 3) { kmpTorService.startTor(any(), any()) }
            coVerify(exactly = 1) { kmpTorService.stopTor(any()) }
        }

    @Test
    fun `activate keeps retrying when a start attempt throws`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            var attempt = 0
            coEvery { kmpTorService.startTor(any(), any()) } coAnswers {
                attempt++
                if (attempt == 1) {
                    throw RuntimeException("tor error")
                } else {
                    torStateFlow.value = KmpTorService.TorState.Started
                    true
                }
            }
            facade.activate()
            coVerify(exactly = 2) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate completes when tor reaches started`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            val activateJob =
                launch {
                    facade.activate()
                }
            runCurrent()
            torStateFlow.value = KmpTorService.TorState.Started
            activateJob.join()
            coVerify(atLeast = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate throws TorBootstrapNotReadyException when bootstrap fails`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            val activateJob =
                launch {
                    assertFailsWith<TorBootstrapNotReadyException> {
                        facade.activate()
                    }
                }
            runCurrent()
            torBootstrapFailedFlow.value = true
            runCurrent()
            activateJob.join()
            coVerify(exactly = 1) { kmpTorService.stopTor(any()) }
        }

    @Test
    fun `activate throws TorBootstrapNotReadyException when await times out without started or failure signal`() =
        runTest(testDispatcher) {
            val facade = createFacade(torEnabled = true)
            val activateJob =
                launch {
                    assertFailsWith<TorBootstrapNotReadyException> {
                        facade.activate()
                    }
                }
            runCurrent()
            advanceTimeBy(2_000L)
            runCurrent()
            advanceTimeBy(KmpTorService.DEFAULT_DAEMON_START_TIMEOUT_MS + KmpTorService.DEFAULT_BOOTSTRAP_TIMEOUT_MS)
            runCurrent()
            activateJob.join()
            coVerify(exactly = 3) { kmpTorService.startTor(any(), any()) }
            coVerify(exactly = 1) { kmpTorService.stopTor(any()) }
        }

    @Test
    fun `activate throws TorBootstrapNotReadyException when bootstrap fails even if stopTor fails`() =
        runTest(testDispatcher) {
            coEvery { kmpTorService.stopTor(any()) } throws RuntimeException("stop failed")
            val facade = createFacade(torEnabled = true)
            val activateJob =
                launch {
                    assertFailsWith<TorBootstrapNotReadyException> {
                        facade.activate()
                    }
                }
            runCurrent()
            torBootstrapFailedFlow.value = true
            runCurrent()
            activateJob.join()
            coVerify(exactly = 1) { kmpTorService.stopTor(any()) }
        }
}

private class TestNetworkServiceFacade(
    kmpTorService: KmpTorService,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val torEnabled: Boolean,
) : NetworkServiceFacade(kmpTorService, applicationBootstrapFacade) {
    override val numConnections: StateFlow<Int> = MutableStateFlow(0)
    override val allDataReceived: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun isTorEnabled(): Boolean = torEnabled
}
