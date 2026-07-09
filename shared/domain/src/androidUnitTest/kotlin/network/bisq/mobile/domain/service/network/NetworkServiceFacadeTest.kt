package network.bisq.mobile.domain.service.network

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.di.testModule
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkServiceFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var kmpTorService: KmpTorService

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        kmpTorService = mockk(relaxed = true)
        startKoin { modules(testModule) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createFacade(torEnabled: Boolean): TestNetworkServiceFacade {
        every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
        return TestNetworkServiceFacade(kmpTorService, torEnabled)
    }

    @Test
    fun `ensureTorRunning starts tor when enabled and stopped`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            facade.ensureTorRunning()
            coVerify(exactly = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning does nothing when tor disabled`() =
        runTest {
            val facade = createFacade(torEnabled = false)
            facade.ensureTorRunning()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning does nothing when tor already started`() =
        runTest {
            every { kmpTorService.state } returns
                MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Started)
            every { kmpTorService.bootstrapProgress } returns MutableStateFlow(100)
            val facade = TestNetworkServiceFacade(kmpTorService, torEnabled = true)
            facade.ensureTorRunning()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `ensureTorRunning catches non-cancellation exception and returns`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } throws RuntimeException("tor error")
            // Should not throw - exception is caught and logged
            facade.ensureTorRunning()
        }

    @Test
    fun `activate does not start tor when disabled`() =
        runTest {
            val facade = createFacade(torEnabled = false)
            facade.activate()
            coVerify(exactly = 0) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate starts tor once when it succeeds first attempt`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            coEvery { kmpTorService.startTor(any(), any()) } returns true
            facade.activate()
            coVerify(exactly = 1) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate retries after a failed start and stops once tor is up`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            // First attempt fails (e.g. stale cookie AUTHENTICATE), second succeeds after cleanup.
            coEvery { kmpTorService.startTor(any(), any()) } returnsMany listOf(false, true)
            facade.activate()
            coVerify(exactly = 2) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate gives up after max attempts and returns without hanging`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            // Always fails: activate must return (not suspend forever waiting for Started).
            coEvery { kmpTorService.startTor(any(), any()) } returns false
            facade.activate()
            // MAX_TOR_START_ATTEMPTS = 3 (private const in NetworkServiceFacade).
            coVerify(exactly = 3) { kmpTorService.startTor(any(), any()) }
        }

    @Test
    fun `activate keeps retrying when a start attempt throws`() =
        runTest {
            val facade = createFacade(torEnabled = true)
            // Non-cancellation throw on first attempt is caught and treated as a failed attempt,
            // then the second attempt succeeds.
            coEvery { kmpTorService.startTor(any(), any()) } throws RuntimeException("tor error") andThen true
            facade.activate()
            coVerify(exactly = 2) { kmpTorService.startTor(any(), any()) }
        }
}

private class TestNetworkServiceFacade(
    kmpTorService: KmpTorService,
    private val torEnabled: Boolean,
) : NetworkServiceFacade(kmpTorService) {
    override val numConnections: StateFlow<Int> = MutableStateFlow(0)
    override val allDataReceived: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun isTorEnabled(): Boolean = torEnabled
}
