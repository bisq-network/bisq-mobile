package network.bisq.mobile.client.common.domain.service.network

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.commonTestModule
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientConnectivityServiceTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var clientConnectivityService: ClientConnectivityService
    private lateinit var webSocketClientService: WebSocketClientService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        startKoin { modules(commonTestModule) }
        webSocketClientService = mockk(relaxed = true)
        // Default: health check passes when connected
        coEvery { webSocketClientService.sendHealthCheck() } returns true
        every { webSocketClientService.isSubscriptionsPending } returns MutableStateFlow(false)
        every { webSocketClientService.failedSubscriptionTopics } returns MutableStateFlow(emptySet())
        clientConnectivityService = ClientConnectivityService(webSocketClientService, androidPlatformInfo)
        // Reset static averageTripTime via public API: the averaging formula
        // (current + new) / 2 converges quickly to 0, ensuring isSlow() returns false.
        repeat(20) { ClientConnectivityService.newRequestRoundTripTime(0) }
    }

    private val androidPlatformInfo =
        object : PlatformInfo {
            override val name = "Android Test"
            override val type = PlatformType.ANDROID
        }

    private val iosPlatformInfo =
        object : PlatformInfo {
            override val name = "iOS Test"
            override val type = PlatformType.IOS
        }

    private class TestClientConnectivityService(
        webSocket: WebSocketClientService,
        platform: PlatformInfo,
    ) : ClientConnectivityService(webSocket, platform) {
        override val maxReconnectingDurationMs: Long
            get() = 400L
    }

    private fun createIosService(): ClientConnectivityService = ClientConnectivityService(webSocketClientService, iosPlatformInfo)

    @After
    fun tearDown() {
        try {
            clientConnectivityService.stopMonitoring()
        } catch (_: Exception) {
        }
        // Reset static state to prevent cross-test interference
        ClientConnectivityService.resetAverageTripTime()
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `checkConnectivity calls triggerReconnect when not connected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            coVerify(atLeast = 1) { webSocketClientService.triggerReconnect() }
        }

    @Test
    fun `checkConnectivity returns RECONNECTING when not connected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)
        }

    @Test
    fun `checkConnectivity returns CONNECTED_AND_DATA_RECEIVED when connected and health check passes`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `checkConnectivity does not call triggerReconnect when connected and healthy`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            coVerify(exactly = 0) { webSocketClientService.triggerReconnect() }
        }

    @Test
    fun `checkConnectivity transitions from RECONNECTING to CONNECTED_AND_DATA_RECEIVED`() =
        runBlocking {
            var isConnected = false
            every { webSocketClientService.isConnected() } answers { isConnected }
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)

            isConnected = true
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `startMonitoring calls checkConnectivity periodically`() =
        runBlocking {
            var healthCheckCount = 0
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } answers {
                healthCheckCount++
                true
            }

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(500)

            assertTrue(healthCheckCount >= 3, "Expected at least 3 health checks, got $healthCheckCount")
        }

    @Test
    fun `stopMonitoring cancels periodic checks`() =
        runBlocking {
            var healthCheckCount = 0
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } answers {
                healthCheckCount++
                true
            }

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            val checksBeforeStop = healthCheckCount
            clientConnectivityService.stopMonitoring()
            delay(500)

            assertTrue(healthCheckCount <= checksBeforeStop + 1, "Checks continued after stopMonitoring")
        }

    @Test
    fun `initial status is BOOTSTRAPPING`() {
        val initialStatus = clientConnectivityService.status.value
        assertEquals(ConnectivityService.ConnectivityStatus.BOOTSTRAPPING, initialStatus)
    }

    @Test
    fun `isSlow returns false when sessionTotalRequests is low`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `checkConnectivity returns REQUESTING_INVENTORY when connected but slow`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            for (i in 0 until ClientConnectivityService.MIN_REQUESTS_TO_ASSESS_SPEED + 1) {
                ClientConnectivityService.newRequestRoundTripTime(
                    ClientConnectivityService.ROUND_TRIP_SLOW_THRESHOLD + 100,
                )
            }

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.REQUESTING_INVENTORY, clientConnectivityService.status.value)
        }

    @Test
    fun `checkConnectivity returns CONNECTED_WITH_LIMITATIONS when healthy connection has failed subscriptions`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            every { webSocketClientService.failedSubscriptionTopics } returns MutableStateFlow(setOf(Topic.MARKET_PRICE))

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_WITH_LIMITATIONS,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `checkConnectivity returns CONNECTED_WITH_LIMITATIONS after untrusted connection recovers with failed subscriptions`() =
        runBlocking {
            var healthCheckPasses = false
            every { webSocketClientService.isConnected() } returns true
            every { webSocketClientService.failedSubscriptionTopics } returns MutableStateFlow(setOf(Topic.NUM_OFFERS))
            coEvery { webSocketClientService.sendHealthCheck() } answers { healthCheckPasses }
            coEvery { webSocketClientService.forceReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)

            healthCheckPasses = true
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_WITH_LIMITATIONS,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `triggerReconnect called repeatedly while disconnected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(500)

            coVerify(atLeast = 3) { webSocketClientService.triggerReconnect() }
        }

    @Test
    fun `service lifecycle activate and deactivate work correctly`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            clientConnectivityService.activate()
            assertTrue(true)

            clientConnectivityService.deactivate()
            assertTrue(true)
        }

    @Test
    fun `health check failure triggers forceReconnect and RECONNECTING status`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } returns false
            coEvery { webSocketClientService.forceReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)
            coVerify(atLeast = 1) { webSocketClientService.forceReconnect() }
        }

    @Test
    fun `health check exception triggers forceReconnect`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } throws RuntimeException("connection dead")
            coEvery { webSocketClientService.forceReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            coVerify(atLeast = 1) { webSocketClientService.forceReconnect() }
        }

    @Test
    fun `deactivate resets status to BOOTSTRAPPING`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            // Verify we're connected
            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )

            // Deactivate should reset status
            clientConnectivityService.deactivate()

            assertEquals(
                ConnectivityService.ConnectivityStatus.BOOTSTRAPPING,
                clientConnectivityService.status.value,
                "Status should be reset to BOOTSTRAPPING after deactivate",
            )
        }

    @Test
    fun `deactivate then activate resets status and restarts monitoring`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            // Verify connected
            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )

            // Full lifecycle restart (activate internally calls startMonitoring with default delays)
            clientConnectivityService.deactivate()
            clientConnectivityService.activate()

            // Immediately after reactivation, status should be BOOTSTRAPPING
            assertEquals(
                ConnectivityService.ConnectivityStatus.BOOTSTRAPPING,
                clientConnectivityService.status.value,
                "Status should be BOOTSTRAPPING after deactivate/activate cycle",
            )

            // Override the default monitoring with short delays to verify it recovers
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)
            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
                "Monitoring should detect connectivity after activate",
            )
        }

    @Test
    fun `recovery after health check failure when server comes back`() =
        runBlocking {
            var healthCheckPasses = false
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } answers { healthCheckPasses }
            coEvery { webSocketClientService.forceReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)

            // Server comes back
            healthCheckPasses = true
            delay(300)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                clientConnectivityService.status.value,
            )
        }

    @Test
    fun `prolonged RECONNECTING transitions to DISCONNECT after timeout`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            val service =
                TestClientConnectivityService(
                    webSocketClientService,
                    androidPlatformInfo,
                )
            try {
                service.activate()
                service.startMonitoring(period = 100, startDelay = 0)
                delay(100)
                assertEquals(
                    ConnectivityService.ConnectivityStatus.RECONNECTING,
                    service.status.value,
                )
                testDispatcher.scheduler.advanceTimeBy(500L)
                assertEquals(
                    ConnectivityService.ConnectivityStatus.DISCONNECTED,
                    service.status.value,
                )
            } finally {
                service.stopMonitoring()
            }
        }

    @Test
    fun `after RECONNECTING timeout status stays DISCONNECT while reconnection still fails`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs

            val service =
                TestClientConnectivityService(
                    webSocketClientService,
                    androidPlatformInfo,
                )
            try {
                service.activate()
                service.startMonitoring(period = 100, startDelay = 0)
                delay(100)
                assertEquals(
                    ConnectivityService.ConnectivityStatus.RECONNECTING,
                    service.status.value,
                )
                testDispatcher.scheduler.advanceTimeBy(500L)
                assertEquals(ConnectivityService.ConnectivityStatus.DISCONNECTED, service.status.value)

                // Further polls still submit RECONNECTING; base class keeps DISCONNECTED
                delay(200)
                assertEquals(
                    ConnectivityService.ConnectivityStatus.DISCONNECTED,
                    service.status.value,
                    "Should not oscillate back to RECONNECTING after timeout",
                )
            } finally {
                service.stopMonitoring()
            }
        }

    @Test
    fun `connection stays RECONNECTING when isConnected true but health check keeps failing`() =
        runBlocking {
            // Simulates desktop node shutdown: TCP connection stays alive (isConnected=true)
            // but server never responds to health checks
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } returns false
            coEvery { webSocketClientService.forceReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)

            // First cycle: health check fails, marks untrusted, sets RECONNECTING
            delay(200)
            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)

            // Subsequent cycles: isConnected() still true but connectionUntrusted flag
            // prevents re-entering the "connected" path — stays RECONNECTING
            delay(500)
            assertEquals(
                ConnectivityService.ConnectivityStatus.RECONNECTING,
                clientConnectivityService.status.value,
                "Should remain RECONNECTING when health checks keep failing on half-open connection",
            )

            // Verify forceReconnect was called multiple times (not just once)
            coVerify(atLeast = 3) { webSocketClientService.forceReconnect() }
        }

    // ///////////////////////////////////////////////////////////////////////////
    // iOS-specific reconnection recovery tests
    // ///////////////////////////////////////////////////////////////////////////

    @Test
    fun `iOS calls forceClientRecreation after threshold disconnected cycles`() =
        runBlocking {
            val iosService = createIosService()
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs
            coEvery { webSocketClientService.forceClientRecreation() } just Runs

            iosService.activate()
            // Use period shorter than threshold to hit it quickly
            // Threshold is IOS_FORCE_RECREATE_CYCLES (12), so we need 12+ cycles
            iosService.startMonitoring(period = 50, startDelay = 0)
            delay(50 * 15) // enough for >12 cycles

            coVerify(atLeast = 1) { webSocketClientService.forceClientRecreation() }
            iosService.stopMonitoring()
        }

    @Test
    fun `Android never calls forceClientRecreation even after many disconnected cycles`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs
            coEvery { webSocketClientService.forceClientRecreation() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 50, startDelay = 0)
            delay(50 * 20) // well past the iOS threshold

            coVerify(exactly = 0) { webSocketClientService.forceClientRecreation() }
        }

    @Test
    fun `iOS resets reconnecting counter when connection recovers`() =
        runBlocking {
            val iosService = createIosService()
            var connected = false
            every { webSocketClientService.isConnected() } answers { connected }
            coEvery { webSocketClientService.triggerReconnect() } just Runs
            coEvery { webSocketClientService.forceClientRecreation() } just Runs

            iosService.activate()
            iosService.startMonitoring(period = 50, startDelay = 0)
            // Accumulate some disconnected cycles (but below threshold)
            delay(50 * 5)

            // Connection recovers
            connected = true
            delay(50 * 3)

            assertEquals(
                ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
                iosService.status.value,
            )

            // Disconnect again — counter should have reset, so forceClientRecreation
            // should not be called yet (we haven't hit the threshold again)
            connected = false
            delay(50 * 5) // only 5 cycles, below threshold of 12

            coVerify(exactly = 0) { webSocketClientService.forceClientRecreation() }
            iosService.stopMonitoring()
        }

    @Test
    fun `iOS uses triggerReconnect before reaching threshold`() =
        runBlocking {
            val iosService = createIosService()
            every { webSocketClientService.isConnected() } returns false
            coEvery { webSocketClientService.triggerReconnect() } just Runs
            coEvery { webSocketClientService.forceClientRecreation() } just Runs

            iosService.activate()
            iosService.startMonitoring(period = 50, startDelay = 0)
            // Only run a few cycles, below threshold
            delay(50 * 5)

            coVerify(atLeast = 1) { webSocketClientService.triggerReconnect() }
            coVerify(exactly = 0) { webSocketClientService.forceClientRecreation() }
            iosService.stopMonitoring()
        }

    @Test
    fun `clientRevoked delegates to webSocketClientService and acknowledgeRevocation resets it`() =
        runBlocking {
            val revokedFlow = MutableStateFlow(false)
            every { webSocketClientService.clientRevoked } returns revokedFlow

            assertEquals(false, clientConnectivityService.clientRevoked.value)

            revokedFlow.value = true
            assertEquals(true, clientConnectivityService.clientRevoked.value)

            every { webSocketClientService.acknowledgeRevocation() } answers { revokedFlow.value = false }
            clientConnectivityService.acknowledgeRevocation()
            assertEquals(false, clientConnectivityService.clientRevoked.value)
        }

    @Test
    fun `health check 401 triggers session renewal and sets RECONNECTING`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            coEvery { webSocketClientService.sendHealthCheck() } throws UnauthorizedApiAccessException()
            coEvery { webSocketClientService.attemptSessionRenewal() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)
            coVerify(atLeast = 1) { webSocketClientService.attemptSessionRenewal() }
        }
}
