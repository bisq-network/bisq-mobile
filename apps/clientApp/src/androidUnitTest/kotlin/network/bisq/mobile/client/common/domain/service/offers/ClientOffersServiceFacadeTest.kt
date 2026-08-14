package network.bisq.mobile.client.common.domain.service.offers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientOffersServiceFacadeTest : ClientKoinIntegrationTestBase() {
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
    private val ignoredProfileIds = MutableStateFlow<Set<String>>(emptySet())
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private lateinit var facade: ClientOffersServiceFacade

    /** Set by [activateWithOffers], for the tests that need to deliver a second NUM_OFFERS event. */
    private lateinit var numOffersObserver: WebSocketEventObserver

    private val usdMarket = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
    private val brlMarket = MarketVO("BTC", "BRL", "Bitcoin", "Brazilian Real")

    override fun onSetup() {
        every { webSocketClientService.connectionState } returns connectionState
        // Neutral default for the REST fast-path so existing tests are unaffected; an empty result
        // is a no-op (the subscription remains the source of truth for empty markets).
        coEvery { apiGateway.getOffers(any()) } returns Result.success(emptyList())
        coEvery { apiGateway.subscribeOffers() } returns WebSocketEventObserver()
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        facade =
            ClientOffersServiceFacade(
                marketPriceServiceFacade = marketPriceServiceFacade,
                apiGateway = apiGateway,
                json = json,
                webSocketClientService = webSocketClientService,
                userProfileServiceFacade = userProfileServiceFacade,
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
    fun `activate subscribes to offers`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeOffers() }
        }

    @Test
    fun `selectOfferbookMarket does not re-subscribe when offers subscription started at activate`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeOffers() }
        }

    @Test
    fun `activate tolerates num offers subscription failure`() =
        runTest {
            coEvery { apiGateway.subscribeNumOffers() } throws RuntimeException("subscribe failed")

            facade.activate()
            advanceUntilIdle()
        }

    @Test
    fun `activate tolerates offers subscription failure`() =
        runTest {
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } throws RuntimeException("subscribe failed")

            facade.activate()
            advanceUntilIdle()
        }

    @Test
    fun `malformed offers event is skipped and the subscription keeps delivering later events`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            offersObserver.setEvent(offersEvent("not-a-valid-offers-payload", sequenceNumber = 1))
            advanceUntilIdle()

            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "good-offer"), sequenceNumber = 2))
            advanceUntilIdle()

            // The malformed event must not tear down the subscription (no re-subscribe) and the
            // following valid event must still populate the offerbook.
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }
            assertEquals(
                "good-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
        }

    @Test
    fun `failed offers subscription at activate allows retry on market select`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } throws RuntimeException("subscribe failed") andThen offersObserver

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            coVerify(exactly = 2) { apiGateway.subscribeOffers() }
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "retry-offer")))
            advanceUntilIdle()

            assertEquals(1, facade.offerbookListItems.value.size)
            assertEquals(
                "retry-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
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
     * After the loading timeout fires, the OFFERS subscription must stay alive and the guard must
     * stay set: re-selecting a market must NOT re-subscribe. Re-subscribing would cancel the
     * in-flight subscription that is about to deliver its snapshot — which is exactly why, before
     * this fix, offers only appeared after navigating back and forth on a slow Tor cold start.
     */
    @Test
    fun `loading timeout keeps the subscription alive and re-selection does not re-subscribe`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            // Loading timeout (30s) fires — spinner stops but the subscription stays alive
            advanceTimeBy(31_000L)
            advanceUntilIdle()
            assertEquals(false, facade.isOfferbookLoading.value)

            // Re-selecting the same market must not trigger a second subscription
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()
            coVerify(exactly = 1) { apiGateway.subscribeOffers() }

            // The late snapshot still lands on the original (alive) subscription and populates
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "late-offer")))
            advanceUntilIdle()
            assertEquals(1, facade.offerbookListItems.value.size)
        }

    /**
     * Distinguishes "NUM_OFFERS not received yet" (unknown) from "confirmed zero": an empty OFFERS
     * snapshot for a market whose count we don't know yet must keep the spinner, not flash a false
     * "no offers".
     */
    @Test
    fun `empty offers snapshot keeps loading while numOffers is unknown`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            // No NUM_OFFERS event delivered → count for BRL is unknown
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            runCurrent()

            offersObserver.setEvent(offersEvent("[]", sequenceNumber = 1))
            runCurrent()

            assertTrue(facade.isOfferbookLoading.value)
        }

    /**
     * Happy path control: when the subscription is alive and serving data, switching
     * markets must NOT re-subscribe (filters are applied in-process against the cache).
     */
    @Test
    fun `subsequent selectOfferbookMarket does not re-subscribe while subscription is alive`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()
            // NUM_OFFERS explicitly reports zero for BRL, so an empty snapshot is authoritative.
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 0}"""))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 0))
            // runCurrent() processes the launches without advancing the virtual clock past
            // the loading timeout — otherwise the timeout would fire first, muddying this
            // happy-path test.
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

    /**
     * Core regression for the "empty offers after cold start over Tor" bug: the initial OFFERS
     * snapshot can arrive AFTER the loading timeout on a slow Tor connection. The timeout must
     * only stop the spinner — it must NOT tear down the subscription — so a late snapshot still
     * populates the list reactively without the user having to re-enter the market.
     */
    @Test
    fun `offers delivered after loading timeout still populate because subscription stays alive`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            // advanceUntilIdle drives past the loading timeout, which must fire and stop the spinner
            advanceUntilIdle()
            assertEquals(false, facade.isOfferbookLoading.value)

            // Late snapshot arrives well after the timeout — the still-alive collector must consume it
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "late-offer")))
            advanceUntilIdle()

            assertEquals(1, facade.offerbookListItems.value.size)
            assertEquals(
                "late-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
        }

    /**
     * Count-aware loading: when the OFFERS snapshot slice for the selected market is empty but
     * NUM_OFFERS says the market has offers, we keep the spinner instead of flashing a false
     * "no offers" — then clear it once the real offers land.
     */
    @Test
    fun `empty offers snapshot keeps loading while numOffers indicates offers exist`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 15}"""))
            connectionState.value = ConnectionState.Connected
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            // runCurrent processes the subscribe launch without advancing the virtual clock
            // past the loading timeout, so the timeout does not pre-empt the count-aware check.
            runCurrent()

            // Empty snapshot slice for BRL, but NUM_OFFERS says 15 → keep the spinner
            offersObserver.setEvent(offersEvent("[]", sequenceNumber = 1))
            runCurrent()
            assertTrue(facade.isOfferbookLoading.value)

            // Real offers arrive → loading clears and the list populates
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "o1"), sequenceNumber = 2))
            runCurrent()
            assertEquals(false, facade.isOfferbookLoading.value)
            assertEquals(1, facade.offerbookListItems.value.size)
        }

    /**
     * Control for the count-aware guard: when NUM_OFFERS says the market has no offers, an empty
     * snapshot is authoritative — loading must clear so the "no offers" state can render.
     */
    @Test
    fun `empty offers snapshot clears loading when numOffers indicates no offers`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 0}"""))
            connectionState.value = ConnectionState.Connected
            advanceUntilIdle()

            // getMarkets runs on a real Dispatchers.Default thread inside the facade and holds
            // offersMutex while publishing, so virtual-time control cannot order it. Await its
            // completion (market list published) before selecting: otherwise its mutex hold can
            // push the loading publish past runCurrent()'s window on loaded CI runners, failing
            // the assert below flakily. Real-time timeout keeps a hang diagnosable.
            withContext(Dispatchers.Default) {
                withTimeout(5_000) { facade.offerbookMarketItems.first { it.isNotEmpty() } }
            }

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 0))
            runCurrent()

            offersObserver.setEvent(offersEvent("[]", sequenceNumber = 1))
            runCurrent()

            assertEquals(false, facade.isOfferbookLoading.value)
        }

    // -------------------------------------------------------------------------
    // Ignored makers
    //
    // An ignored maker's offer is not a valid offerbook entry — the same rule the node applies in
    // `isValidOfferbookMessage`. Filtering here rather than in the presenter is what makes ignoring
    // take effect immediately, since the presenter's pipeline has no reason to re-run on its own.
    // -------------------------------------------------------------------------

    /**
     * Drives the facade to "market selected, offers delivered", in the order the longer-form tests
     * in this class use: the markets collector only starts once the test dispatcher has run, so
     * `advanceUntilIdle` has to come before anything blocks the thread waiting on it.
     */
    private suspend fun TestScope.activateWithOffers(
        payload: String,
        numOffers: Int,
    ): WebSocketEventObserver {
        val numOffersObserver = WebSocketEventObserver().also { this@ClientOffersServiceFacadeTest.numOffersObserver = it }
        val offersObserver = WebSocketEventObserver()
        coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
        coEvery { apiGateway.subscribeOffers() } returns offersObserver
        coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

        facade.activate()
        advanceUntilIdle()
        connectionState.value = ConnectionState.Connected
        waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }
        numOffersObserver.setEvent(numOffersEvent("""{"BRL": $numOffers}"""))
        advanceUntilIdle()

        facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = numOffers))
        runCurrent()
        offersObserver.setEvent(offersEvent(payload, sequenceNumber = 1))
        return offersObserver
    }

    /**
     * The ignore set is already populated when NUM_OFFERS arrives, i.e. before the OFFERS snapshot
     * has filled the cache — so at that moment there is nothing to subtract and the raw count is
     * published. The count must be corrected once the snapshot lands, or it stays above the visible
     * list and `advertised > offers.size` keeps the offerbook behind a sync spinner for good.
     */
    @Test
    fun `offers from an already ignored maker are never published and the count follows the snapshot`() =
        runTest {
            ignoredProfileIds.value = setOf("maker-a")

            activateWithOffers(offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b"), numOffers = 2)
            advanceUntilIdle()

            // `waitUntil` rather than a bare assertion throughout these tests: the facade publishes
            // from its own (real) scope, which advanceUntilIdle does not settle, so sampling races
            // the publish.
            waitUntil { facade.offerbookListItems.value.map { it.offerId } == listOf("o2") }
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 1
            }
        }

    @Test
    fun `ignoring a maker removes their offer from the published list`() =
        runTest {
            activateWithOffers(offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b"), numOffers = 2)
            advanceUntilIdle()
            waitUntil { facade.offerbookListItems.value.size == 2 }

            ignoredProfileIds.value = setOf("maker-a")
            advanceUntilIdle()

            waitUntil { facade.offerbookListItems.value.map { it.offerId } == listOf("o2") }
        }

    /**
     * The per-market cache keeps every offer and the filter is applied on publish, so un-ignoring
     * restores the offer immediately rather than waiting for the next snapshot or refetch.
     */
    @Test
    fun `un-ignoring a maker restores their offer`() =
        runTest {
            ignoredProfileIds.value = setOf("maker-a")
            activateWithOffers(offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b"), numOffers = 2)
            advanceUntilIdle()
            waitUntil { facade.offerbookListItems.value.size == 1 }

            ignoredProfileIds.value = emptySet()
            advanceUntilIdle()

            waitUntil {
                facade.offerbookListItems.value
                    .map { it.offerId }
                    .sorted() == listOf("o1", "o2")
            }
        }

    /**
     * The count-aware spinner is judged on the unfiltered snapshot: NUM_OFFERS is counted by the
     * node, which knows nothing about local ignores. Judging it on the filtered list would leave a
     * market whose offers all come from ignored makers loading forever instead of showing "no offers".
     */
    @Test
    fun `loading clears even when every offer in the market is from an ignored maker`() =
        runTest {
            ignoredProfileIds.value = setOf("maker-a")

            activateWithOffers(offersPayloadByMaker(brlMarket, "o1" to "maker-a"), numOffers = 1)
            advanceUntilIdle()

            waitUntil { !facade.isOfferbookLoading.value }
            assertTrue(facade.offerbookListItems.value.isEmpty())
        }

    /**
     * NUM_OFFERS and OFFERS are separate topics, so a take-down can reach us as a lower node count
     * before the delta drops the offer from the cache. Subtracting a still-cached ignored offer from
     * that already-reduced count would report fewer offers than the published list is showing —
     * here, one, while two are on screen.
     */
    @Test
    fun `a reduced node count never publishes fewer offers than the list shows`() =
        runTest {
            ignoredProfileIds.value = setOf("maker-a")

            // Advertised 5 against 3 cached (one ignored) so the count starts at 4 and the assertion
            // below pins a real transition rather than a value that was already correct.
            activateWithOffers(
                offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b", "o3" to "maker-c"),
                numOffers = 5,
            )
            advanceUntilIdle()
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 4
            }

            // The node dropped maker-a's offer: its count is down to 2 while our cache still holds it.
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 2}""", sequenceNumber = 2))
            advanceUntilIdle()

            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 2
            }
            assertEquals(
                listOf("o2", "o3"),
                facade.offerbookListItems.value
                    .map { it.offerId }
                    .sorted(),
            )
        }

    /**
     * REST fast-path: on market selection we fetch the market's offers directly, which returns
     * without waiting for the OFFERS subscription snapshot.
     */
    @Test
    fun `rest prefetch populates offers before the subscription snapshot arrives`() =
        runTest {
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns WebSocketEventObserver() // never delivers
            coEvery { apiGateway.getOffers("BRL") } returns Result.success(listOf(buildOfferDto("rest-offer", brlMarket)))

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            assertEquals(1, facade.offerbookListItems.value.size)
            assertEquals(
                "rest-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
            assertEquals(false, facade.isOfferbookLoading.value)
        }

    /**
     * The REST fast-path is best-effort: if the endpoint fails (e.g. not supported by the node),
     * we fall back to the OFFERS subscription exactly as before.
     */
    @Test
    fun `rest prefetch failure is tolerated and the subscription still populates`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getOffers("BRL") } throws RuntimeException("endpoint not available")

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "sub-offer")))
            advanceUntilIdle()

            assertEquals(1, facade.offerbookListItems.value.size)
            assertEquals(
                "sub-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
        }

    /**
     * The OFFERS subscription remains the source of truth: when its snapshot arrives it replaces the
     * REST-prefetched head-start data for that market.
     */
    @Test
    fun `subscription snapshot overrides the rest prefetched offers`() =
        runTest {
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns WebSocketEventObserver()
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getOffers("BRL") } returns Result.success(listOf(buildOfferDto("rest-offer", brlMarket)))

            facade.activate()
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 15))
            advanceUntilIdle()

            // REST head-start populated first
            assertEquals(
                "rest-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )

            // Authoritative subscription snapshot (REPLACE) supersedes the REST data
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "sub-offer")))
            advanceUntilIdle()

            assertEquals(1, facade.offerbookListItems.value.size)
            assertEquals(
                "sub-offer",
                facade.offerbookListItems.value
                    .single()
                    .offerId,
            )
        }

    /**
     * Regression for "created offer not visible until re-entering the market": NUM_OFFERS bumps the
     * count promptly, but the OFFERS ADDED push can lag on a slow connection. On reselect, a count
     * mismatch (cached offers != NUM_OFFERS) reconciles the cache via REST so the new offer shows.
     */
    @Test
    fun `reselecting a market with a stale cache count reconciles via rest`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver

            facade.activate()
            advanceUntilIdle()

            // First cold select: the subscription delivers a single offer for BRL
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 1))
            advanceUntilIdle()
            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "o1"), sequenceNumber = 1))
            advanceUntilIdle()
            assertEquals(1, facade.offerbookListItems.value.size)

            // NUM_OFFERS now reports 2 (an offer was just created) but the OFFERS ADDED hasn't arrived
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 2}"""))
            advanceUntilIdle()

            // Reselect → count mismatch (cached 1 vs numOffers 2) → REST reconcile returns the real 2
            coEvery { apiGateway.getOffers("BRL") } returns
                Result.success(listOf(buildOfferDto("o1", brlMarket), buildOfferDto("o2", brlMarket)))
            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 2))
            advanceUntilIdle()

            assertEquals(2, facade.offerbookListItems.value.size)
            assertEquals(
                setOf("o1", "o2"),
                facade.offerbookListItems.value
                    .map { it.offerId }
                    .toSet(),
            )
        }

    /**
     * The market list advertises NUM_OFFERS; entering a market whose OFFERS snapshot hasn't landed
     * yet must expose a syncing state (drives an honest loading UI instead of a false "no offers"
     * against the advertised count), clearing once the cached offers catch up.
     */
    @Test
    fun `syncing state is exposed while advertised count exceeds cached offers and clears when they arrive`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 1}"""))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 1))
            runCurrent()
            // Advertised 1, cached 0 -> the snapshot is pending and the syncing state must expose it
            facade.isSyncingSelectedMarketOffers
                .filter { it }
                .first()

            offersObserver.setEvent(offersEvent(offersPayload(brlMarket, "o1"), sequenceNumber = 1))
            advanceUntilIdle()
            // Cached offers caught up with the advertised count -> syncing clears
            facade.isSyncingSelectedMarketOffers
                .filter { !it }
                .first()
        }

    /**
     * NUM_OFFERS is counted by the node, which knows nothing about who this device ignores. Ignoring
     * a maker shrinks the published list, so unless the advertised count shrinks with it the facade
     * reads "offers still inbound" forever and the offerbook is stranded behind a sync spinner.
     *
     * Asserted on the two inputs rather than on [isSyncingSelectedMarketOffers] itself, which is
     * `advertised > offers.size` (`OffersServiceFacade`) and therefore false exactly when these are
     * equal. The flow cannot be asserted on directly here: this module's Koin graph binds a real
     * `DefaultCoroutineJobsManager`, so the facade emits off the test scheduler, and the flow's
     * `WhileSubscribed` resets to its initial `false` between collections — an assertion on it
     * passes against the very regression this test exists to catch. The wiring of the flow itself is
     * covered by `syncing state is exposed while advertised count exceeds cached offers…` above.
     */
    @Test
    fun `advertised count matches the visible list after a maker is ignored`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 2}"""))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 2))
            runCurrent()
            offersObserver.setEvent(
                offersEvent(offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b"), sequenceNumber = 1),
            )
            advanceUntilIdle()
            waitUntil { facade.offerbookListItems.value.size == 2 }
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 2
            }

            ignoredProfileIds.value = setOf("maker-a")
            advanceUntilIdle()

            // Equal on both sides => `advertised > offers.size` is false => no sync spinner.
            waitUntil { facade.offerbookListItems.value.size == 1 }
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 1
            }
        }

    @Test
    fun `market offer count drops while a maker is ignored and returns when un-ignored`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 2}"""))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 2))
            runCurrent()
            offersObserver.setEvent(
                offersEvent(offersPayloadByMaker(brlMarket, "o1" to "maker-a", "o2" to "maker-b"), sequenceNumber = 1),
            )
            advanceUntilIdle()
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 2
            }

            ignoredProfileIds.value = setOf("maker-a")
            advanceUntilIdle()
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 1
            }

            ignoredProfileIds.value = emptySet()
            advanceUntilIdle()
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 2
            }
        }

    /** The subtraction must not underflow, and the spinner must still clear on an all-ignored market. */
    @Test
    fun `market with only ignored makers reports zero offers and clears loading`() =
        runTest {
            val numOffersObserver = WebSocketEventObserver()
            val offersObserver = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumOffers() } returns numOffersObserver
            coEvery { apiGateway.subscribeOffers() } returns offersObserver
            coEvery { apiGateway.getMarkets() } returns Result.success(listOf(brlMarket))

            facade.activate()
            advanceUntilIdle()
            connectionState.value = ConnectionState.Connected
            waitUntil { facade.offerbookMarketItems.value.isNotEmpty() }
            numOffersObserver.setEvent(numOffersEvent("""{"BRL": 1}"""))
            advanceUntilIdle()

            facade.selectOfferbookMarket(MarketListItem.from(brlMarket, numOffers = 1))
            runCurrent()
            offersObserver.setEvent(offersEvent(offersPayloadByMaker(brlMarket, "o1" to "maker-a"), sequenceNumber = 1))
            advanceUntilIdle()

            ignoredProfileIds.value = setOf("maker-a")
            advanceUntilIdle()

            waitUntil { facade.offerbookListItems.value.isEmpty() }
            waitUntil {
                facade.offerbookMarketItems.value
                    .singleOrNull()
                    ?.numOffers == 0
            }
            waitUntil { !facade.isOfferbookLoading.value }
        }

    private fun offersEvent(
        payload: String,
        sequenceNumber: Int = 1,
        modificationType: ModificationType = ModificationType.REPLACE,
    ) = WebSocketEvent(
        topic = Topic.OFFERS,
        subscriberId = "offers-test",
        deferredPayload = payload,
        modificationType = modificationType,
        sequenceNumber = sequenceNumber,
    )

    private fun offersPayload(
        market: MarketVO,
        vararg offerIds: String,
    ): String = json.encodeToString(offerIds.map { buildOfferDto(it, market) })

    /** Payload where each offer is authored by its own maker, so ignoring can be targeted. */
    private fun offersPayloadByMaker(
        market: MarketVO,
        vararg offerIdToMakerId: Pair<String, String>,
    ): String = json.encodeToString(offerIdToMakerId.map { (offerId, makerId) -> buildOfferDto(offerId, market, makerId) })

    private fun buildOfferDto(
        id: String,
        market: MarketVO,
        makerId: String = "id",
    ): OfferItemPresentationDto {
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = makerId),
            )
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.BUY,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(100_00L),
                priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(100_00L, market)),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = false,
            userProfile = createMockUserProfile("Alice"),
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = "",
            formattedPriceSpec = "",
            quoteSidePaymentMethods = emptyList(),
            baseSidePaymentMethods = emptyList(),
            reputationScore = ReputationScoreVO(0, 0.0, 0),
        )
    }

    /**
     * [WebSocketEventObserver.setEvent] drops anything whose sequence number does not advance, so a
     * test delivering a second NUM_OFFERS must bump this — otherwise the event is silently ignored.
     */
    private fun numOffersEvent(
        payload: String,
        sequenceNumber: Int = 1,
    ) = WebSocketEvent(
        topic = Topic.NUM_OFFERS,
        subscriberId = "num-offers-test",
        deferredPayload = payload,
        modificationType = ModificationType.REPLACE,
        sequenceNumber = sequenceNumber,
    )

    /**
     * Real-time polling, deliberately, and the only place in this file that blocks: the facade
     * publishes from its own `serviceScope`, and clientApp's test module binds a real
     * [network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager] rather than one running on
     * [testDispatcher] — so `advanceUntilIdle()` does not settle that work and sampling a StateFlow
     * straight after feeding an event races the publish. The timeout is wall-clock for the same
     * reason; virtual time says nothing about a scope it does not drive.
     */
    private fun waitUntil(
        timeoutMs: Long = 5_000L,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for condition" }
            Thread.sleep(5)
        }
    }
}
