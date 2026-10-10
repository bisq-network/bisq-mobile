package network.bisq.mobile.client.common.domain.service.trades

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeStallClockRepository
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.trades.ExpectedTradeProtocolRejection
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    private lateinit var stallClockRepository: TradeStallClockRepository
    private lateinit var facade: ClientTradesServiceFacade
    private val capabilities = MutableStateFlow(BackendCapabilities(setOf(Feature.BANNED_ACCOUNT_DATA.key)))
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk { every { this@mockk.capabilities } returns this@ClientTradesServiceFacadeTest.capabilities }

    override fun onSetup() {
        apiGateway = mockk(relaxed = true)
        webSocketClientService = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        analyticsService = mockk(relaxed = true)
        stallClockRepository = mockk(relaxed = true)
        facade =
            ClientTradesServiceFacade(
                apiGateway,
                webSocketClientService,
                Json,
                globalUiManager,
                analyticsService,
                stallClockRepository,
                backendCapabilitiesService,
            )
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

    private val capturedErrors = mutableListOf<Pair<String, Throwable?>>()

    /** Set on the instance, not via `Logger.setLogWriters`: see the facade's `log` declaration. */
    private fun captureErrorLogs() {
        capturedErrors.clear()
        facade.log =
            Logger(
                loggerConfigInit(
                    object : LogWriter() {
                        override fun log(
                            severity: Severity,
                            message: String,
                            tag: String,
                            throwable: Throwable?,
                        ) {
                            if (severity == Severity.Error) capturedErrors.add(message to throwable)
                        }
                    },
                ),
                tag = "ClientTradesServiceFacade",
            )
    }

    /**
     * In client mode the failure message is the trusted node's 400 body, which can name the peer's
     * profile or account data. The log gets a classification only, and no throwable: a cause chain
     * would carry the same body into the sink. The user-facing message keeps the body-derived text.
     */
    @Test
    fun `takeOffer failure logs a classification and not the response body`() =
        runTest {
            I18nSupport.initialize("en")
            val body = "Invalid input: An error occurred at the peers side at taking the offer: peer profile 3f9a2c1d rejected"
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.BadRequest, body))
            captureErrorLogs()
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

            assertEquals(1, capturedErrors.size)
            val (message, throwable) = capturedErrors.single()
            assertEquals("Failed to take offer: WebSocketRestApiException(http=400)", message)
            assertNull(throwable)
            assertFalse(message.contains("3f9a2c1d"), message)
            assertTrue(errorMessage.value!!.contains("3f9a2c1d"), errorMessage.value)
        }

    @Test
    fun `takeOffer restriction failure logs the restriction kind only`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("Trading is on halt for security reasons. Alert id 7b2e"))
            captureErrorLogs()

            facade.takeOffer(
                mockk<BisqEasyOfferVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                mockk<MonetaryVO>(relaxed = true),
                "btc",
                "fiat",
                MutableStateFlow(null),
                MutableStateFlow(null),
            )

            assertEquals("Failed to take offer: RuntimeException restriction=TradingHalted", capturedErrors.single().first)
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

            assertEquals(raw, ExpectedTradeProtocolRejection.extractExpected(errorMessage.value!!))
            assertTrue(ExpectedTradeProtocolRejection.isAtPeer(errorMessage.value!!))
            assertFalse(errorMessage.value!!.contains("ErrorStackTrace"), errorMessage.value)
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

    @Test
    fun `account data is not banned and not checked when the node lacks the feature`() =
        runTest {
            capabilities.value = BackendCapabilities.UNAVAILABLE

            assertFalse(facade.isAccountDataBanned(ACCOUNT_DATA))

            coVerify(exactly = 0) { apiGateway.isAccountDataBanned(any()) }
        }

    @Test
    fun `account data banned check returns the node's answer for the selected trade`() =
        runTest {
            coEvery { apiGateway.isAccountDataBanned(TRADE_ID) } returns Result.success(AccountDataBannedResponse(banned = true))

            withSelectedTrade {
                assertTrue(facade.isAccountDataBanned(ACCOUNT_DATA))
            }
        }

    @Test
    fun `account data banned check returns false when the node finds the data clean`() =
        runTest {
            coEvery { apiGateway.isAccountDataBanned(TRADE_ID) } returns Result.success(AccountDataBannedResponse(banned = false))

            withSelectedTrade {
                assertFalse(facade.isAccountDataBanned(ACCOUNT_DATA))
            }
        }

    @Test
    fun `account data banned check throws when the node call fails`() =
        runTest {
            coEvery { apiGateway.isAccountDataBanned(TRADE_ID) } returns Result.failure(IllegalStateException("offline"))

            withSelectedTrade {
                assertFailsWith<IllegalStateException> { facade.isAccountDataBanned(ACCOUNT_DATA) }
            }
        }

    @Test
    fun `a timed out account data banned check throws a failure that is not a cancellation`() =
        runTest {
            // What WebSocketApiClient returns for a request timeout.
            val timeout = assertIs<TimeoutCancellationException>(runCatching { withTimeout(1) { awaitCancellation() } }.exceptionOrNull())
            coEvery { apiGateway.isAccountDataBanned(TRADE_ID) } returns Result.failure(timeout)

            withSelectedTrade {
                val thrown = assertFailsWith<Throwable> { facade.isAccountDataBanned(ACCOUNT_DATA) }
                // The caller is not cancelled, so the timeout must surface as a failure.
                assertFalse(thrown is CancellationException)
            }
        }

    @Test
    fun `banned account data cancel calls the api and tracks the automatic reason`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.success(Unit)
            // A known stall age, so the UNKNOWN bucket below is the facade's choice and not a missing clock.
            coEvery { stallClockRepository.fetch() } returns
                TradeStallClockMap(mapOf(TRADE_ID to TradeStallClockEntry(BisqEasyTradeStateEnum.INIT.name, transitionAtMs = 0)))

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)

                coVerify(exactly = 1) { apiGateway.cancelTrade(TRADE_ID) }
                verify {
                    analyticsService.track(
                        AnalyticsEvent.Trade.Cancelled(
                            AnalyticsEvent.Trade.InterruptReason.BANNED_ACCOUNT_DATA,
                            AnalyticsEvent.Trade.StallBucket.UNKNOWN,
                        ),
                    )
                }
            }
        }

    /** Runs [block] on an activated facade whose selected trade is [TRADE_ID]. */
    private suspend fun TestScope.withSelectedTrade(block: suspend () -> Unit) {
        mockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        try {
            val trade = mockk<TradeItemPresentationModel>(relaxed = true)
            every { trade.tradeId } returns TRADE_ID
            every { any<TradeItemPresentationDto>().toDomain() } returns trade
            coEvery { webSocketClientService.subscribe(any(), any()) } returns WebSocketEventObserver()
            facade.activate()
            runCurrent()
            facade.handleTradeItemPresentationChange(listOf(mockk()), ModificationType.REPLACE)
            facade.selectOpenTrade(TRADE_ID)

            block()
        } finally {
            // Always: a failed assertion would otherwise leave the analytics ticker spinning.
            facade.deactivate()
            unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        }
    }

    private companion object {
        const val TRADE_ID = "trade-1"
        const val ACCOUNT_DATA = "IBAN: DE89370400440532013000"

        /** JVM file class holding the `TradeItemPresentationDto.toDomain` extension. */
        const val TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS =
            "network.bisq.mobile.client.common.domain.service.trades.TradeItemPresentationDtoMappingKt"
    }
}
