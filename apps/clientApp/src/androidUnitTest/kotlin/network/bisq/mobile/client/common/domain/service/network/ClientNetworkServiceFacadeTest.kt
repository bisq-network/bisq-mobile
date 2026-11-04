package network.bisq.mobile.client.common.domain.service.network

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers the NETWORK_INFO subscription collector of [ClientNetworkServiceFacade]: a valid event
 * updates [ClientNetworkServiceFacade.networkInfo], and [ClientNetworkServiceFacade.allDataReceived]
 * follows the node's own sync flag.
 *
 * The collector launches on the injected `dispatcherProvider.default`, here backed by
 * [testDispatcher] — the same pattern as `ClientOffersServiceFacadeTest` — so delivery settles
 * with `advanceUntilIdle()`. Tor stays disabled (`SensitiveSettingsRepositoryMock` defaults to
 * `BisqProxyOption.NONE`) so activation skips the Tor bootstrap path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientNetworkServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val networkApiGateway: NetworkApiGateway = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val httpClientService: HttpClientService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val firstDataReceived = MutableStateFlow(false)
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)
    private val failedTopics = MutableStateFlow<Set<Topic>>(emptySet())
    private var sequenceNumber = 0
    private lateinit var facade: ClientNetworkServiceFacade

    override fun onSetup() {
        // Read when the facade is built, so they must be stubbed first.
        every { webSocketClientService.initialSubscriptionsReceivedData } returns firstDataReceived
        every { webSocketClientService.failedSubscriptionTopics } returns failedTopics
        every { webSocketClientService.connectionState } returns connectionState
        facade =
            ClientNetworkServiceFacade(
                SensitiveSettingsRepositoryMock(),
                httpClientService,
                webSocketClientService,
                networkApiGateway,
                json,
                kmpTorService,
                applicationBootstrapFacade,
                StandardTestDispatcherProvider(testDispatcher),
            )
    }

    @Test
    fun `first data is not enough while the node has not answered`() =
        runTest {
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns WebSocketEventObserver()
            facade.activate()
            observeAllDataReceived()

            firstDataReceived.value = true
            advanceUntilIdle()

            assertFalse(facade.allDataReceived.value)
        }

    @Test
    fun `all data is received once a syncing node reports it finished`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns observer
            facade.activate()
            observeAllDataReceived()
            firstDataReceived.value = true

            observer.setNetworkInfo(allDataReceived = false)
            advanceUntilIdle()
            assertFalse(facade.allDataReceived.value)

            observer.setNetworkInfo(allDataReceived = true)
            advanceUntilIdle()
            assertTrue(facade.allDataReceived.value)
        }

    @Test
    fun `first data is enough when the node refuses the network info subscription`() =
        runTest {
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns WebSocketEventObserver()
            facade.activate()
            observeAllDataReceived()

            failedTopics.value = setOf(Topic.NETWORK_INFO)
            firstDataReceived.value = true
            advanceUntilIdle()

            assertTrue(facade.allDataReceived.value)
        }

    @Test
    fun `after a restart all data follows the new data and not the node's old answer`() =
        runTest {
            val firstObserver = WebSocketEventObserver()
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns firstObserver
            facade.activate()
            observeAllDataReceived()
            firstDataReceived.value = true
            firstObserver.setNetworkInfo(allDataReceived = true)
            advanceUntilIdle()
            assertTrue(facade.allDataReceived.value)

            facade.deactivate()
            firstDataReceived.value = false
            advanceUntilIdle()
            assertFalse(facade.allDataReceived.value)

            coEvery { networkApiGateway.subscribeNetworkInfo() } returns WebSocketEventObserver()
            facade.activate()
            firstDataReceived.value = true
            advanceUntilIdle()
            assertFalse(facade.allDataReceived.value)

            facade.deactivate()
        }

    @Test
    fun `after a reconnect all data waits for the node's new answer`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns observer
            facade.activate()
            observeAllDataReceived()
            firstDataReceived.value = true
            observer.setNetworkInfo(allDataReceived = true)
            advanceUntilIdle()
            assertTrue(facade.allDataReceived.value)

            connectionState.value = ConnectionState.Disconnected()
            firstDataReceived.value = false
            advanceUntilIdle()
            connectionState.value = ConnectionState.Connected
            firstDataReceived.value = true
            advanceUntilIdle()
            assertFalse(facade.allDataReceived.value)

            observer.setNetworkInfo(allDataReceived = true)
            advanceUntilIdle()
            assertTrue(facade.allDataReceived.value)

            facade.deactivate()
        }

    @Test
    fun `network info websocket event updates networkInfo`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { networkApiGateway.subscribeNetworkInfo() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.NETWORK_INFO,
                    subscriberId = "network-info-test",
                    deferredPayload =
                        """
                        {
                          "allDataReceived": true,
                          "torRunning": false,
                          "myAddress": "abc123.onion:8090",
                          "keyId": "key-1",
                          "connections": []
                        }
                        """.trimIndent(),
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            val info = facade.networkInfo.value
            assertNotNull(info)
            assertEquals(true, info.allDataReceived)
            assertEquals(false, info.torRunning)
            assertEquals("abc123.onion:8090", info.myAddress)
            assertEquals("key-1", info.keyId)
            assertEquals(0, info.connections.size)

            facade.deactivate()
        }

    // The value is shared lazily: without a collector it never leaves its initial false.
    // runCurrent, as advanceUntilIdle does not run work that is only in the background scope.
    private fun TestScope.observeAllDataReceived() {
        backgroundScope.launch(testDispatcher) { facade.allDataReceived.collect {} }
        runCurrent()
    }

    private suspend fun WebSocketEventObserver.setNetworkInfo(allDataReceived: Boolean) {
        setEvent(
            WebSocketEvent(
                topic = Topic.NETWORK_INFO,
                subscriberId = "network-info-test",
                deferredPayload = """{"allDataReceived": $allDataReceived, "torRunning": false, "connections": []}""",
                modificationType = ModificationType.REPLACE,
                sequenceNumber = ++sequenceNumber,
            ),
        )
    }
}
