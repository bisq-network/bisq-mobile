package network.bisq.mobile.client.common.domain.service.trades

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeCancelReasonRepository
import network.bisq.mobile.domain.repository.TradeStallClockRepository
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.trades.ExpectedTradeProtocolRejection
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
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
    private lateinit var cancelReasonRepository: TradeCancelReasonRepository
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
        cancelReasonRepository = mockk(relaxed = true)
        facade =
            ClientTradesServiceFacade(
                apiGateway,
                webSocketClientService,
                Json,
                globalUiManager,
                analyticsService,
                stallClockRepository,
                cancelReasonRepository,
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
                facade.handleTradeItemPresentationChange(listOf(tradeDto("trade-1")), ModificationType.REPLACE)
                assertEquals(listOf("trade-1"), facade.openTradeItems.value.map { it.tradeId })
                assertTrue(facade.openTradesSynced.value)

                facade.deactivate()

                assertTrue(facade.openTradeItems.value.isEmpty())
                assertFalse(facade.openTradesSynced.value)
            } finally {
                unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
            }
        }

    /** The node removes a trade from the open trades when it moves it to the closed trades. */
    @Test
    fun `a removed open trade refreshes the closed trades`() =
        runTest {
            withOpenTradeDtos("trade-1") { (dto) ->
                facade.handleTradeItemPresentationChange(listOf(dto), ModificationType.REPLACE)
                val tickBefore = facade.closedTradesChangeTick.value

                facade.handleTradeItemPresentationChange(listOf(dto), ModificationType.REMOVED)

                assertTrue(facade.openTradeItems.value.isEmpty())
                assertEquals(tickBefore + 1, facade.closedTradesChangeTick.value)
            }
        }

    @Test
    fun `a snapshot without a previously open trade refreshes the closed trades`() =
        runTest {
            withOpenTradeDtos("trade-1", "trade-2") { (first, second) ->
                facade.handleTradeItemPresentationChange(listOf(first, second), ModificationType.REPLACE)
                val tickBefore = facade.closedTradesChangeTick.value

                // The reconnect snapshot after trade-1 was closed while the app was disconnected.
                facade.handleTradeItemPresentationChange(listOf(second), ModificationType.REPLACE)

                assertEquals(listOf("trade-2"), facade.openTradeItems.value.map { it.tradeId })
                assertEquals(tickBefore + 1, facade.closedTradesChangeTick.value)
            }
        }

    @Test
    fun `added trades and an unchanged snapshot leave the closed trades alone`() =
        runTest {
            withOpenTradeDtos("trade-1", "trade-2") { (first, second) ->
                facade.handleTradeItemPresentationChange(listOf(first), ModificationType.REPLACE)
                val tickBefore = facade.closedTradesChangeTick.value

                facade.handleTradeItemPresentationChange(listOf(second), ModificationType.ADDED)
                facade.handleTradeItemPresentationChange(listOf(first, second), ModificationType.REPLACE)

                assertEquals(tickBefore, facade.closedTradesChangeTick.value)
            }
        }

    /**
     * Screens, the selected trade, the trade chat and the notification observers all hold the trade object,
     * so a reconnect snapshot must update it instead of replacing it with a new one.
     */
    @Test
    fun `a reconnect snapshot keeps the open trade objects and updates their state`() =
        runTest {
            withFreshMappingDtos { dtoFor ->
                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, INITIAL_STATE)), ModificationType.REPLACE)
                val original = facade.openTradeItems.value.single()
                facade.selectOpenTrade(TRADE_ID)

                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, LATER_STATE)), ModificationType.REPLACE)

                assertSame(original, facade.openTradeItems.value.single())
                assertSame(original, facade.selectedTrade.value)
                verify { original.bisqEasyTradeModel.setTradeState(LATER_STATE) }
            }
        }

    /** A snapshot is the full state, so a value the node no longer has must be cleared, not kept. */
    @Test
    fun `a reconnect snapshot clears values the node no longer has`() =
        runTest {
            withFreshMappingDtos { dtoFor ->
                facade.handleTradeItemPresentationChange(
                    listOf(dtoFor(TRADE_ID, INITIAL_STATE).withPeersError("peer failed")),
                    ModificationType.REPLACE,
                )
                val tradeModel =
                    facade.openTradeItems.value
                        .single()
                        .bisqEasyTradeModel

                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, LATER_STATE)), ModificationType.REPLACE)

                verify { tradeModel.setPeersErrorMessage(null) }
                // A state can imply the data, so observers must not see it before the data.
                verifyOrder {
                    tradeModel.setPaymentAccountData(any())
                    tradeModel.setTradeState(LATER_STATE)
                }
            }
        }

    /**
     * The trade analytics react to an error message by reading its stack trace right away, and report each
     * trade once, so an error first received in a snapshot must find its stack trace already set.
     */
    @Test
    fun `a reconnect snapshot sets each stack trace before its error message`() =
        runTest {
            withFreshMappingDtos { dtoFor ->
                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, INITIAL_STATE)), ModificationType.REPLACE)
                val tradeModel =
                    facade.openTradeItems.value
                        .single()
                        .bisqEasyTradeModel

                facade.handleTradeItemPresentationChange(
                    listOf(dtoFor(TRADE_ID, LATER_STATE).withPeersError("peer failed")),
                    ModificationType.REPLACE,
                )

                verifyOrder {
                    tradeModel.setErrorStackTrace(any())
                    tradeModel.setErrorMessage(any())
                    tradeModel.setPeersErrorStackTrace(any())
                    tradeModel.setPeersErrorMessage("peer failed")
                }
            }
        }

    @Test
    fun `a reconnect snapshot adds new trades in the snapshot order and drops the gone ones`() =
        runTest {
            withFreshMappingDtos { dtoFor ->
                facade.handleTradeItemPresentationChange(
                    listOf(dtoFor("trade-1", INITIAL_STATE), dtoFor("trade-2", INITIAL_STATE)),
                    ModificationType.REPLACE,
                )
                val kept = facade.openTradeItems.value.first { it.tradeId == "trade-2" }

                facade.handleTradeItemPresentationChange(
                    listOf(dtoFor("trade-3", INITIAL_STATE), dtoFor("trade-2", LATER_STATE)),
                    ModificationType.REPLACE,
                )

                assertEquals(listOf("trade-3", "trade-2"), facade.openTradeItems.value.map { it.tradeId })
                assertSame(kept, facade.openTradeItems.value[1])
            }
        }

    @Test
    fun `an added event for a known trade updates it instead of replacing it`() =
        runTest {
            withFreshMappingDtos { dtoFor ->
                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, INITIAL_STATE)), ModificationType.REPLACE)
                val original = facade.openTradeItems.value.single()

                facade.handleTradeItemPresentationChange(listOf(dtoFor(TRADE_ID, LATER_STATE)), ModificationType.ADDED)

                assertSame(original, facade.openTradeItems.value.single())
                verify { original.bisqEasyTradeModel.setTradeState(LATER_STATE) }
            }
        }

    /** bisq2 pushes one property per event, so properties cached before the trade arrives must all be kept. */
    @Test
    fun `every property cached before the trade arrives is applied`() =
        runTest {
            withOpenTradeDtos(TRADE_ID) { (dto) ->
                val earlierState = BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST
                val state = BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA
                facade.handleTradePropertiesChange(listOf(mapOf(TRADE_ID to TradePropertiesDto(tradeState = earlierState))), ModificationType.REPLACE)
                facade.handleTradePropertiesChange(
                    listOf(mapOf(TRADE_ID to TradePropertiesDto(paymentAccountData = ACCOUNT_DATA))),
                    ModificationType.REPLACE,
                )
                facade.handleTradePropertiesChange(listOf(mapOf(TRADE_ID to TradePropertiesDto(tradeState = state))), ModificationType.REPLACE)

                facade.handleTradeItemPresentationChange(listOf(dto), ModificationType.ADDED)

                val tradeModel =
                    facade.openTradeItems.value
                        .single()
                        .bisqEasyTradeModel
                // The state says the account data arrived, so observers must not see it before the data.
                verifyOrder {
                    tradeModel.setPaymentAccountData(ACCOUNT_DATA)
                    tradeModel.setTradeState(state)
                }
                // Replaying a superseded state would move the trade backwards.
                verify(exactly = 0) { tradeModel.setTradeState(earlierState) }
            }
        }

    /** Both handlers read and write the pending-properties cache, so one must not run while the other is mid-way. */
    @Test
    fun `a trades event waits for a trade properties event still being handled`() =
        runTest {
            withOpenTradeDtos(TRADE_ID) { (dto) ->
                val tradesObserver = WebSocketEventObserver()
                val propertiesObserver = WebSocketEventObserver()
                coEvery { webSocketClientService.subscribe(Topic.TRADES, any()) } returns tradesObserver
                coEvery { webSocketClientService.subscribe(Topic.TRADE_PROPERTIES, any()) } returns propertiesObserver
                facade.handleTradeItemPresentationChange(listOf(dto), ModificationType.REPLACE)
                val applying = CountDownLatch(1)
                val release = CountDownLatch(1)
                every {
                    facade.openTradeItems.value
                        .single()
                        .bisqEasyTradeModel
                        .setTradeState(any())
                } answers {
                    applying.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    Unit
                }
                try {
                    facade.activate()
                    runCurrent()

                    propertiesObserver.setEvent(event(Topic.TRADE_PROPERTIES, """[{"$TRADE_ID":{"tradeState":"INIT"}}]"""))
                    assertTrue(applying.await(5, TimeUnit.SECONDS))
                    // A snapshot without the trade, arriving while its property is still being applied.
                    tradesObserver.setEvent(event(Topic.TRADES, "[]"))
                    Thread.sleep(200)
                    assertEquals(listOf(TRADE_ID), facade.openTradeItems.value.map { it.tradeId })

                    release.countDown()
                    withContext(Dispatchers.Default) { withTimeout(5_000) { facade.openTradeItems.first { it.isEmpty() } } }
                } finally {
                    release.countDown()
                    facade.deactivate()
                }
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

    /**
     * A 2.1.14+ node reports why the take is refused as an errorCode in the 400 body. The maker's
     * outdated version is not something the user or their trusted node admin can fix, so the message
     * points at another offer instead of an upgrade.
     */
    @Test
    fun `takeOffer maker version error code maps to take another offer guidance`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(
                        HttpStatusCode.BadRequest,
                        "Invalid input: The maker of this offer uses a Bisq version below 2.1.14. " +
                            "The Bisq security manager has published an emergency alert which requires this version for " +
                            "trading, so the maker cannot accept the take offer request.",
                        errorCode = "MAKER_VERSION_BELOW_MIN_REQUIRED",
                        minRequiredVersion = "2.1.14",
                    ),
                )
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

            assertEquals(
                "mobile.bisqEasy.takeOffer.makerVersionBelowMinRequired".i18n("2.1.14"),
                errorMessage.value,
            )
            assertEquals(
                "Failed to take offer: WebSocketRestApiException(http=400) restriction=MakerVersionBelowMinRequired",
                capturedErrors.single().first,
            )
        }

    @Test
    fun `takeOffer error code wins over the text of the body`() =
        runTest {
            I18nSupport.initialize("en")
            // The text alone would read as the node's own min-version restriction.
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(
                        HttpStatusCode.BadRequest,
                        "Invalid input: For trading you need to have version 2.1.14 installed. " +
                            "The Bisq security manager has published an emergency alert with a min. version required for trading.",
                        errorCode = "HALT_TRADING",
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

            assertEquals("mobile.bisqEasy.takeOffer.tradingHalted".i18n(), errorMessage.value)
        }

    @Test
    fun `takeOffer unknown error code falls back to the raw message`() =
        runTest {
            I18nSupport.initialize("en")
            coEvery { apiGateway.takeOffer(any(), any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(
                        HttpStatusCode.BadRequest,
                        "Invalid input: A restriction this app does not know yet",
                        errorCode = "SOME_FUTURE_RESTRICTION",
                        minRequiredVersion = "9.9.9",
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

            assertEquals("Invalid input: A restriction this app does not know yet", errorMessage.value)
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

    @Test
    fun `banned account data cancel saves the reason for the trade`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.success(Unit)

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)

                coVerify(exactly = 1) { cancelReasonRepository.setReason(TRADE_ID, TradeCancelReason.BANNED_ACCOUNT_DATA) }
            }
        }

    @Test
    fun `banned account data cancel saves the reason before sending the cancel and keeps it`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.success(Unit)

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)

                coVerifyOrder {
                    cancelReasonRepository.setReason(TRADE_ID, TradeCancelReason.BANNED_ACCOUNT_DATA)
                    apiGateway.cancelTrade(TRADE_ID)
                }
                coVerify(exactly = 0) { cancelReasonRepository.clearId(any()) }
            }
        }

    @Test
    fun `a failed banned account data cancel removes the reason it saved`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.failure(IllegalStateException("offline"))

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isFailure)

                coVerifyOrder {
                    cancelReasonRepository.setReason(TRADE_ID, TradeCancelReason.BANNED_ACCOUNT_DATA)
                    cancelReasonRepository.clearId(TRADE_ID)
                }
            }
        }

    @Test
    fun `a banned account data cancel still succeeds when saving the reason fails`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.success(Unit)
            coEvery { cancelReasonRepository.setReason(any(), any()) } throws IllegalStateException("disk full")

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)
            }
        }

    @Test
    fun `a user cancel saves no reason`() =
        runTest {
            coEvery { apiGateway.cancelTrade(TRADE_ID) } returns Result.success(Unit)

            withSelectedTrade {
                assertTrue(facade.cancelTrade(AnalyticsEvent.Trade.InterruptReason.PRICE_MOVED).isSuccess)

                coVerify(exactly = 0) { cancelReasonRepository.setReason(any(), any()) }
            }
        }

    /** A trade DTO that only answers its trade id, which is what the trades handler matches on. */
    private fun tradeDto(tradeId: String): TradeItemPresentationDto = mockk { every { trade.id } returns tradeId }

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
            facade.handleTradeItemPresentationChange(listOf(tradeDto(TRADE_ID)), ModificationType.REPLACE)
            facade.selectOpenTrade(TRADE_ID)

            block()
        } finally {
            // Always: a failed assertion would otherwise leave the analytics ticker spinning.
            facade.deactivate()
            unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        }
    }

    /** Runs [block] with one trade DTO per id, each mapping to an open trade with that id. */
    private suspend fun withOpenTradeDtos(
        vararg tradeIds: String,
        block: suspend (List<TradeItemPresentationDto>) -> Unit,
    ) {
        mockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        try {
            val dtos =
                tradeIds.map { tradeId ->
                    val trade = mockk<TradeItemPresentationModel>(relaxed = true)
                    every { trade.tradeId } returns tradeId
                    val dto = mockk<TradeItemPresentationDto>(relaxed = true)
                    every { dto.trade.id } returns tradeId
                    every { dto.toDomain() } returns trade
                    dto
                }
            block(dtos)
        } finally {
            unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        }
    }

    /**
     * Runs [block] with a factory of trade DTOs whose mapping builds a new trade object on every call, like the
     * real mapping does, so a test can tell an updated object from a replaced one.
     */
    private suspend fun withFreshMappingDtos(block: suspend ((String, BisqEasyTradeStateEnum) -> TradeItemPresentationDto) -> Unit) {
        mockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        try {
            block { tradeId, state ->
                val tradeDto = mockk<BisqEasyTradeDto>(relaxed = true)
                every { tradeDto.id } returns tradeId
                every { tradeDto.tradeState } returns state
                every { tradeDto.peersErrorMessage } returns null
                val dto = mockk<TradeItemPresentationDto>(relaxed = true)
                every { dto.trade } returns tradeDto
                every { dto.toDomain() } answers {
                    mockk<TradeItemPresentationModel>(relaxed = true) {
                        every { this@mockk.tradeId } returns tradeId
                        every { shortTradeId } returns tradeId.take(8)
                    }
                }
                dto
            }
        } finally {
            unmockkStatic(TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS)
        }
    }

    // Stubbed on the trade DTO itself: a chained `every { trade.peersErrorMessage }` replaces `trade` with a new
    // child mock that has no trade id.
    private fun TradeItemPresentationDto.withPeersError(message: String): TradeItemPresentationDto {
        val tradeDto = trade
        every { tradeDto.peersErrorMessage } returns message
        return this
    }

    private fun event(
        topic: Topic,
        payload: String,
    ) = WebSocketEvent(
        topic = topic,
        subscriberId = "test-subscriber",
        deferredPayload = payload,
        modificationType = ModificationType.REPLACE,
        sequenceNumber = 1,
    )

    private companion object {
        const val TRADE_ID = "trade-1"
        const val ACCOUNT_DATA = "IBAN: DE89370400440532013000"
        val INITIAL_STATE = BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST
        val LATER_STATE = BisqEasyTradeStateEnum.TAKER_RECEIVED_TAKE_OFFER_RESPONSE__BUYER_DID_NOT_SENT_BTC_ADDRESS__BUYER_RECEIVED_ACCOUNT_DATA

        /** JVM file class holding the `TradeItemPresentationDto.toDomain` extension. */
        const val TRADE_ITEM_PRESENTATION_DTO_MAPPING_CLASS =
            "network.bisq.mobile.client.common.domain.service.trades.TradeItemPresentationDtoMappingKt"
    }
}
