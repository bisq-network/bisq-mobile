package network.bisq.mobile.client.common.domain.service.trades

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the analytics wiring [ClientTradesServiceFacade] gets from [BaseTradesServiceFacade]:
 * `takeOffer` → `Taken`, the `trackedAction`-wrapped confirm steps, and `observeTradesForAnalytics`
 * on activation. The confirm/reject/cancel calls run without a selected trade, so `requireNotNull`
 * throws — but the tracked-action wiring executes first, which is what we're exercising here.
 */
class ClientTradesServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private lateinit var apiGateway: TradesApiGateway
    private lateinit var webSocketClientService: WebSocketClientService
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var analyticsService: AnalyticsService
    private lateinit var facade: ClientTradesServiceFacade

    override fun onSetup() {
        apiGateway = mockk(relaxed = true)
        webSocketClientService = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        analyticsService = mockk(relaxed = true)
        facade = ClientTradesServiceFacade(apiGateway, webSocketClientService, Json, globalUiManager, analyticsService, mockk(relaxed = true))
    }

    @Test
    fun `a trades snapshot marks the open trades as synced`() =
        runTest {
            assertFalse(facade.openTradesSynced.value, "Nothing has been delivered yet")

            // The snapshot the node sends on subscribe, empty here because only the flag is under test.
            facade.handleTradeItemPresentationChange(emptyList(), ModificationType.REPLACE)

            assertTrue(facade.openTradesSynced.value)
        }

    @Test
    fun `an incremental trades update does not mark the open trades as synced`() =
        runTest {
            facade.handleTradeItemPresentationChange(emptyList(), ModificationType.ADDED)

            assertFalse(facade.openTradesSynced.value, "Only the snapshot is authoritative about absence")
        }

    /** A subscribe that failed is only retried on the next reconnect, so a wait on the sync has to be told. */
    @Test
    fun `a failed trades subscription is reported until a reconnect clears it`() =
        runTest {
            val failedTopics = MutableStateFlow<Set<Topic>>(emptySet())
            every { webSocketClientService.failedSubscriptionTopics } returns failedTopics
            coEvery { webSocketClientService.subscribe(any(), any()) } returns WebSocketEventObserver()
            facade.activate()
            runCurrent()
            assertFalse(facade.openTradesSyncFailed.value)

            failedTopics.value = setOf(Topic.TRADES)
            runCurrent()
            assertTrue(facade.openTradesSyncFailed.value)

            failedTopics.value = emptySet()
            runCurrent()
            assertFalse(facade.openTradesSyncFailed.value)

            facade.deactivate()
        }

    /** After re-pairing, a deep link must resolve against the new session, not the previous one's trades. */
    @Test
    fun `deactivate drops the open trades together with the synced flag`() =
        runTest {
            mockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
            try {
                val trade = mockk<TradeItemPresentationModel>(relaxed = true)
                every { trade.tradeId } returns "trade-1"
                every { any<TradeItemPresentationDto>().toDomain() } returns trade
                facade.handleTradeItemPresentationChange(listOf(mockk()), ModificationType.REPLACE)
                assertEquals(listOf("trade-1"), facade.openTradeItems.value.map { it.tradeId })
                assertTrue(facade.openTradesSynced.value)

                facade.deactivate()

                assertTrue(facade.openTradeItems.value.isEmpty())
                assertFalse(facade.openTradesSynced.value)
            } finally {
                unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
            }
        }

    @Test
    fun `takeOffer success tracks Taken`() =
        runTest {
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns Result.success(mockk(relaxed = true))

            val result =
                facade.takeOffer(
                    mockk<BisqEasyOfferVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    "btc",
                    "fiat",
                    MutableStateFlow(null),
                    MutableStateFlow(null),
                )

            assertTrue(result.isSuccess)
            verify { analyticsService.track(AnalyticsEvent.Trade.Taken) }
        }

    @Test
    fun `takeOffer failure does not track Taken`() =
        runTest {
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns Result.failure(RuntimeException("nope"))

            val result =
                facade.takeOffer(
                    mockk<BisqEasyOfferVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    "btc",
                    "fiat",
                    MutableStateFlow(null),
                    MutableStateFlow(null),
                )

            assertTrue(result.isFailure)
            verify(exactly = 0) { analyticsService.track(AnalyticsEvent.Trade.Taken) }
        }

    /**
     * A bare failure Result used to leave takeOfferErrorMessage null, so the presenter kept the
     * blocking "taking offer" dialog up forever. Any failure must populate the error flow.
     */
    @Test
    fun `takeOffer failure populates the error message flow`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns Result.failure(RuntimeException("nope"))
            val errorMessage = MutableStateFlow<String?>(null)

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                errorMessage,
            )

            assertTrue(errorMessage.value!!.contains("nope"), "raw reason should be surfaced, got: ${errorMessage.value}")
        }

    @Test
    fun `takeOffer price deviation rejection is passed through unwrapped`() =
        runTest {
            I18nSupport.initialize("en")
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException(raw))
            val errorMessage = MutableStateFlow<String?>(null)

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                errorMessage,
            )

            assertEquals(raw, errorMessage.value)
        }

    /**
     * TradeRestApi.takeOffer reports a peer rejection as a plain-text 400 with an
     * `. ErrorStackTrace:` tail. The dialog must show the core protocol sentence only.
     */
    @Test
    fun `takeOffer price deviation rejection strips the REST ErrorStackTrace tail`() =
        runTest {
            I18nSupport.initialize("en")
            val raw =
                "Takers (buyers) Bitcoin amount is too high. " +
                    "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                    "to manipulate the price."
            val restBody =
                "Invalid input: An error occurred at the peers side at taking the offer: $raw. " +
                    "ErrorStackTrace: bisq.trade.exceptions.TradeProtocolException: $raw" +
                    "\n\tat bisq.trade.bisq_easy.protocol.BisqEasyProtocol.onMessage(BisqEasyProtocol.java:1)" +
                    "\n\tat java.base/java.lang.Thread.run(Thread.java:1)"
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException(restBody))
            val errorMessage = MutableStateFlow<String?>(null)

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                errorMessage,
            )

            assertEquals(raw, errorMessage.value)
        }

    /**
     * Security-manager min-version rejection (the node refuses trading because IT runs a version
     * below the emergency alert's minimum) must surface as guidance to contact the trusted node
     * admin, not as raw backend text.
     */
    @Test
    fun `takeOffer min-version security rejection maps to trusted node upgrade guidance`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(
                    RuntimeException(
                        "Invalid input: For trading you need to have version 2.1.12 installed. " +
                            "The Bisq security manager has published an emergency alert with a min. version required for trading.",
                    ),
                )
            val errorMessage = MutableStateFlow<String?>(null)

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                errorMessage,
            )

            val message = errorMessage.value!!
            assertTrue(message.contains("2.1.12"), "required version should be named, got: $message")
            assertTrue(message.contains("trusted node"), "should point at the trusted node, got: $message")
        }

    @Test
    fun `takeOffer halt-trading security rejection maps to halted message`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(
                    RuntimeException(
                        "Invalid input: Trading is on halt for security reasons. " +
                            "The Bisq security manager has published an emergency alert with haltTrading set to true",
                    ),
                )
            val errorMessage = MutableStateFlow<String?>(null)

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                errorMessage,
            )

            assertTrue(errorMessage.value!!.contains("halted trading"), "got: ${errorMessage.value}")
        }

    @Test
    fun `activate wires subscriptions and launches the analytics observers`() =
        runTest {
            coEvery { webSocketClientService.subscribe(any(), any()) } returns WebSocketEventObserver()
            facade.activate()
            // runCurrent, NOT advanceUntilIdle: activation starts the analytics out-of-sync recheck
            // ticker (TimeUtils.tickerFlow), and an infinite ticker keeps the virtual-time scheduler
            // busy forever — advanceUntilIdle would spin. Same constraint as OpenTradePresenterTest.
            runCurrent()
            facade.deactivate()
        }

    @Test
    fun `confirm steps run the tracked-action wiring`() =
        runTest {
            // No selected trade → requireNotNull(tradeId) throws, but trackedAction + the apiGateway call execute first.
            assertFailsWith<IllegalArgumentException> { facade.sellerSendsPaymentAccount("data") }
            assertFailsWith<IllegalArgumentException> { facade.buyerSendBitcoinPaymentData("addr") }
            assertFailsWith<IllegalArgumentException> { facade.sellerConfirmFiatReceipt() }
            assertFailsWith<IllegalArgumentException> { facade.buyerConfirmFiatSent() }
            assertFailsWith<IllegalArgumentException> { facade.sellerConfirmBtcSent(null) }
            assertFailsWith<IllegalArgumentException> { facade.btcConfirmed() }
        }

    @Test
    fun `reject and cancel run without a selected trade`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { facade.rejectTrade() }
            assertFailsWith<IllegalArgumentException> { facade.cancelTrade() }
        }

    private companion object {
        /** JVM file class holding the `TradeItemPresentationDto.toDomain` extension. */
        const val TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS =
            "network.bisq.mobile.client.common.domain.service.trades.TradeItemPresentationDtoMappingKt"
    }
}
