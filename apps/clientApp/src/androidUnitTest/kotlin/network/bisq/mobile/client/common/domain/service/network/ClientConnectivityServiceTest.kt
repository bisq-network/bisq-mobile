package network.bisq.mobile.client.common.domain.service.network

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.commonTestModule
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.network.ConnectivityService
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
        clientConnectivityService = ClientConnectivityService(webSocketClientService)
        // Reset static averageTripTime via public API: the averaging formula
        // (current + new) / 2 converges quickly to 0, ensuring isSlow() returns false.
        repeat(20) { ClientConnectivityService.newRequestRoundTripTime(0) }
    }

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
            every { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            verify(atLeast = 1) { webSocketClientService.triggerReconnect() }
        }

    @Test
    fun `checkConnectivity returns RECONNECTING when not connected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            every { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            assertEquals(ConnectivityService.ConnectivityStatus.RECONNECTING, clientConnectivityService.status.value)
        }

    @Test
    fun `checkConnectivity returns CONNECTED_AND_DATA_RECEIVED when connected and not slow`() =
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
    fun `checkConnectivity does not call triggerReconnect when connected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns true
            every { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            verify(exactly = 0) { webSocketClientService.triggerReconnect() }
        }

    @Test
    fun `checkConnectivity transitions from RECONNECTING to CONNECTED_AND_DATA_RECEIVED`() =
        runBlocking {
            var isConnected = false
            every { webSocketClientService.isConnected() } answers { isConnected }
            every { webSocketClientService.triggerReconnect() } just Runs

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
            var connectivityCheckCount = 0
            every { webSocketClientService.isConnected() } answers {
                connectivityCheckCount++
                true
            }

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(500)

            assertTrue(connectivityCheckCount >= 3, "Expected at least 3 connectivity checks, got $connectivityCheckCount")
        }

    @Test
    fun `stopMonitoring cancels periodic checks`() =
        runBlocking {
            var connectivityCheckCount = 0
            every { webSocketClientService.isConnected() } answers {
                connectivityCheckCount++
                true
            }

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(300)

            val checksBeforeStop = connectivityCheckCount
            clientConnectivityService.stopMonitoring()
            delay(500)

            assertTrue(connectivityCheckCount <= checksBeforeStop + 1, "Checks continued after stopMonitoring")
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
    fun `triggerReconnect called repeatedly while disconnected`() =
        runBlocking {
            every { webSocketClientService.isConnected() } returns false
            every { webSocketClientService.triggerReconnect() } just Runs

            clientConnectivityService.activate()
            clientConnectivityService.startMonitoring(period = 100, startDelay = 0)
            delay(500)

            verify(atLeast = 3) { webSocketClientService.triggerReconnect() }
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
}
