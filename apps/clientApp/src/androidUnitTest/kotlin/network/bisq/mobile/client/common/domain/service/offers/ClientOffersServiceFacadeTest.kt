package network.bisq.mobile.client.common.domain.service.offers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientOffersServiceFacadeTest : KoinIntegrationTestBase() {
    private val marketPriceServiceFacade =
        object : MarketPriceServiceFacade(mockk(relaxed = true)) {
            override fun findMarketPriceItem(marketVO: MarketVO) = null

            override fun findUSDMarketPriceItem() = null

            override fun refreshSelectedFormattedMarketPrice() {}

            override fun selectMarket(marketListItem: MarketListItem) = Result.success(Unit)
        }
    private val apiGateway: OfferbookApiGateway = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    private lateinit var facade: ClientOffersServiceFacade

    private val usdMarket = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
    private val brlMarket = MarketVO("BTC", "BRL", "Bitcoin", "Brazilian Real")

    override fun onSetup() {
        every { webSocketClientService.connectionState } returns connectionState
        facade =
            ClientOffersServiceFacade(
                marketPriceServiceFacade = marketPriceServiceFacade,
                apiGateway = apiGateway,
                json = json,
                webSocketClientService = webSocketClientService,
            )
    }

    @Test
    fun `activate subscribes to num offers`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver

            facade.activate()
            advanceUntilIdle()

            coVerify { apiGateway.subscribeNumOffers() }
        }

    @Test
    fun `activate tolerates num offers subscription failure`() =
        runTest {
            coEvery { apiGateway.subscribeNumOffers() } throws RuntimeException("subscribe failed")

            facade.activate()
            advanceUntilIdle()
        }

    @Test
    fun `num offers websocket event updates market list`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(usdMarket))

            facade.activate()
            advanceUntilIdle()

            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }

            numOffersObserver.setEvent(numOffersEvent("""{"USD": 5}"""))
            advanceUntilIdle()

            assertEquals(
                5,
                facade.offerbookMarketItems.value
                    .single()
                    .numOffers,
            )
        }

    @Test
    fun `cached num offers replayed when markets load after websocket snapshot`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(usdMarket))

            facade.activate()
            advanceUntilIdle()

            numOffersObserver.setEvent(numOffersEvent("""{"USD": 7}"""))
            advanceUntilIdle()

            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }

            assertEquals(
                7,
                facade.offerbookMarketItems.value
                    .single()
                    .numOffers,
            )
        }

    @Test
    fun `deactivate clears offerbook markets`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver

            facade.activate()
            advanceUntilIdle()
            numOffersObserver.setEvent(numOffersEvent("""{"USD": 3}"""))
            advanceUntilIdle()

            facade.deactivate()
            advanceUntilIdle()

            assertTrue(facade.offerbookMarketItems.value.isEmpty())
        }

    /**
     * Regression test for the "stuck subscription guard" bug: if the initial OFFERS
     * subscription request never produces a payload (e.g. trusted node misbehaving),
     * the loading timeout fires — and historically `hasSubscribedToOffers` stayed
     * `true`, leaving every subsequent `selectOfferbookMarket` call stuck showing
     * zero offers forever (until app restart).
     */
    @Test
    fun `loading timeout releases the guard so next selectOfferbookMarket re-subscribes`() =
        runTest {
            val firstObserver = WebSocketEventObserver()
            val secondObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returnsMany listOf(firstObserver, secondObserver)

            facade.activate()
            advanceUntilIdle()

            // First market selection — subscribes (and server never delivers an OFFERS payload)
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            // Loading timeout (12s) fires — guard must be released
            advanceTimeBy(13_000L)
            advanceUntilIdle()

            // Second market selection — must re-subscribe instead of being permanently stuck
            facade.selectOfferbookMarket(MarketListItem.from(usdMarket, numOffers = 82))
            advanceUntilIdle()
            coVerify(exactly = 2) { apiGateway.subscribeOffers() }
        }

    /**
     * Happy path control: when the subscription is alive and serving data, switching
     * markets must NOT re-subscribe (filters are applied in-process against the cache).
     */
    @Test
    fun `subsequent selectOfferbookMarket does not re-subscribe while subscription is alive`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            // runCurrent() processes the launches without advancing the virtual clock past
            // the 12s loading timeout — otherwise the timeout would fire first and reset
            // the guard, defeating the point of this happy-path test.
            runCurrent()

            // Delivering an event causes applyOffersToSelectedMarket to set isLoading=false
            // and cancel the timeout, so the timeout will not fire even after advanceUntilIdle.
            offersObserver.setEvent(offersEvent("[]"))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(usdMarket, numOffers = 82))
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeOffers() }
        }

    /**
     * The subscription collector job and loading timeout job must be cancelled on
     * `deactivate()` so they don't linger past the facade's lifecycle.
     */
    @Test
    fun `deactivate cancels active subscription so re-activate cleanly re-subscribes`() =
        runTest {
            val firstObserver = WebSocketEventObserver()
            val secondObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returnsMany listOf(firstObserver, secondObserver)

            facade.activate()
            advanceUntilIdle()
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            facade.deactivate()
            advanceUntilIdle()

            facade.activate()
            advanceUntilIdle()
            facade.selectOfferbookMarket(MarketListItem.from(usdMarket, numOffers = 82))
            advanceUntilIdle()

            coVerify(exactly = 2) { apiGateway.subscribeOffers() }
        }

    private fun offersEvent(payload: String) =
        WebSocketEvent(
            topic = Topic.OFFERS,
            subscriberId = "offers-test",
            deferredPayload = payload,
            modificationType = ModificationType.REPLACE,
            sequenceNumber = 1,
        )

    private fun numOffersEvent(payload: String) =
        WebSocketEvent(
            topic = Topic.NUM_OFFERS,
            subscriberId = "num-offers-test",
            deferredPayload = payload,
            modificationType = ModificationType.REPLACE,
            sequenceNumber = 1,
        )

    private fun waitUntil(
        timeoutMs: Long = 2_000L,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for condition" }
            Thread.sleep(5)
        }
    }
}
