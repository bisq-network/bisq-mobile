package network.bisq.mobile.client.common.domain.service.network

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
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
import kotlin.test.assertNotNull

/**
 * Covers the NETWORK_INFO subscription collector of [ClientNetworkServiceFacade]: a valid event
 * updates [ClientNetworkServiceFacade.networkInfo].
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
    private lateinit var facade: ClientNetworkServiceFacade

    override fun onSetup() {
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
}
