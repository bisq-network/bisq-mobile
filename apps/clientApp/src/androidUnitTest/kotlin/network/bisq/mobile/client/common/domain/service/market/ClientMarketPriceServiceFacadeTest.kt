package network.bisq.mobile.client.common.domain.service.market

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Covers the MARKET_PRICE subscription collector of [ClientMarketPriceServiceFacade]: a valid
 * event reaches the handler and fires the global price-update trigger.
 *
 * The collector launches on the injected `dispatcherProvider.default`, here backed by
 * [testDispatcher] — the same pattern as `ClientOffersServiceFacadeTest` — so delivery settles
 * with `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientMarketPriceServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: MarketPriceApiGateway = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientMarketPriceServiceFacade

    override fun onSetup() {
        facade =
            ClientMarketPriceServiceFacade(
                apiGateway,
                json,
                SettingsRepositoryMock(),
                StandardTestDispatcherProvider(testDispatcher),
            )
    }

    @Test
    fun `market price websocket event triggers global price update`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeMarketPrice() } returns observer

            facade.activate()
            advanceUntilIdle()
            assertEquals(0L, facade.globalPriceUpdate.value)

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.MARKET_PRICE,
                    subscriberId = "market-price-test",
                    deferredPayload = """{}""",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertNotEquals(0L, facade.globalPriceUpdate.value)

            facade.deactivate()
        }
}
