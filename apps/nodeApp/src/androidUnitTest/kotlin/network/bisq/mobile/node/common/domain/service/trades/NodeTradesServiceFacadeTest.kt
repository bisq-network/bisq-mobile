package network.bisq.mobile.node.common.domain.service.trades

import bisq.account.payment_method.BitcoinPaymentMethod
import bisq.account.payment_method.BitcoinPaymentMethodSpec
import bisq.account.payment_method.BitcoinPaymentRail
import bisq.bisq_easy.BisqEasyService
import bisq.chat.ChatService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.monetary.Monetary
import bisq.common.observable.collection.ObservableSet
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.trade.TradeRestrictedException
import bisq.trade.TradeService
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeService
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState
import bisq.user.UserService
import bisq.user.profile.UserProfile
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeCancelReasonRepository
import network.bisq.mobile.domain.repository.TradeStallClockRepository
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.TradeItemPresentationModelFactory
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel as Bisq2BisqEasyOpenTradeChannel

/**
 * Pins the contract [NodeTradesServiceFacade.activate] rests on: Bisq 2 collection observers replay the
 * existing elements synchronously while binding, so the open trades are complete, and can be marked
 * synced, by the time activate() returns. A real [ObservableSet] stands in for the node's trades so an
 * upstream change to that replay fails here instead of quietly reviving the cold-start race.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeTradesServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private val trades = ObservableSet<BisqEasyTrade>()
    private val channelService: BisqEasyOpenTradeChannelService = mockk(relaxed = true)
    private val offerbookChannels = ObservableSet<BisqEasyOfferbookChannel>()
    private val bisqEasyService: BisqEasyService = mockk()
    private val analyticsService: AnalyticsService = mockk(relaxed = true)
    private val stallClockRepository: TradeStallClockRepository = mockk(relaxed = true)
    private val cancelReasonRepository: TradeCancelReasonRepository = mockk(relaxed = true)
    private lateinit var facade: NodeTradesServiceFacade
    private lateinit var bisqEasyTradeService: BisqEasyTradeService
    private lateinit var applicationService: AndroidApplicationService

    override fun onSetup() {
        bisqEasyTradeService = mockk<BisqEasyTradeService>(relaxed = true)
        every { bisqEasyTradeService.trades } returns trades
        val tradeService = mockk<TradeService>()
        every { tradeService.bisqEasyTradeService } returns bisqEasyTradeService
        val chatService = mockk<ChatService>()
        every { chatService.bisqEasyOpenTradeChannelService } returns channelService
        val offerbookChannelService = mockk<BisqEasyOfferbookChannelService>()
        every { offerbookChannelService.channels } returns offerbookChannels
        every { chatService.bisqEasyOfferbookChannelService } returns offerbookChannelService

        applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.tradeService } returns tradeService
        every { applicationService.chatService } returns chatService
        every { applicationService.bisqEasyService } returns bisqEasyService
        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        mockkObject(TradeItemPresentationModelFactory)
        facade = NodeTradesServiceFacade(provider, analyticsService, stallClockRepository, cancelReasonRepository)
    }

    override fun onTearDown() {
        try {
            unmockkObject(TradeItemPresentationModelFactory)
        } finally {
            super.onTearDown()
        }
    }

    private val capturedErrors = mutableListOf<Pair<String, Throwable?>>()

    /** Set on the instance rather than through `Logger.setLogWriters`: see the facade's `log` declaration. */
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
                tag = "NodeTradesServiceFacade",
            )
    }

    /**
     * On the node the failure is bisq2's own exception, whose message quotes the peer's protocol
     * text. The log gets the class chain only and no throwable; the user-facing flow keeps the text.
     */
    @Test
    fun `takeOffer failure logs a classification and not the exception message`() =
        runTest {
            val peerText = "An error occurred at the peers side at taking the offer: peer profile 3f9a2c1d rejected"
            val (offerVO, _) = givenOfferInOfferbook()
            mockkObject(Mappings.MonetaryMapping)
            try {
                every { Mappings.MonetaryMapping.toBisq2Model(any()) } throws IllegalStateException(peerText)
                captureErrorLogs()
                val errorMessage = MutableStateFlow<String?>(null)

                val result =
                    facade.takeOffer(
                        offerVO,
                        mockk<MonetaryVO>(relaxed = true),
                        mockk<MonetaryVO>(relaxed = true),
                        "btc",
                        "fiat",
                        MutableStateFlow(null),
                        errorMessage,
                    )

                assertTrue(result.isFailure)
                val (message, throwable) = capturedErrors.single()
                // withContext may rethrow a stack-trace-recovered copy with the original as cause,
                // so the chain can read "IllegalStateException <- IllegalStateException".
                assertTrue(message.startsWith("Failed to take offer: IllegalStateException"), message)
                assertNull(throwable)
                assertFalse(message.contains("3f9a2c1d"), message)
                assertTrue(errorMessage.value.orEmpty().contains("3f9a2c1d"), errorMessage.value)
            } finally {
                unmockkObject(Mappings.MonetaryMapping)
            }
        }

    /** Publishes an offer in the node's offerbook; returns the app's copy of it and the published offer. */
    private fun givenOfferInOfferbook(offerId: String = "offer-1"): Pair<BisqEasyOfferVO, BisqEasyOffer> {
        val publishedOffer = mockk<BisqEasyOffer>(relaxed = true) { every { id } returns offerId }
        val message =
            mockk<BisqEasyOfferbookMessage> {
                every { hasBisqEasyOffer() } returns true
                every { bisqEasyOffer } returns Optional.of(publishedOffer)
            }
        val channel = mockk<BisqEasyOfferbookChannel> { every { chatMessages } returns ObservableSet(listOf(message)) }
        offerbookChannels.add(channel)
        val offerVO = mockk<BisqEasyOfferVO>(relaxed = true) { every { id } returns offerId }
        return offerVO to publishedOffer
    }

    /** Stubs the services doTakeOffer reads before it hands the offer to the core. */
    private fun givenTakeOfferPrerequisites() {
        I18nSupport.initialize("en")
        every { applicationService.userService } returns mockk<UserService>(relaxed = true)
        every { applicationService.bondedRolesService.marketPriceService.findMarketPrice(any()) } returns Optional.empty()
        every {
            applicationService.supportService.bisqEasyMediationRequestService.selectMediator(any(), any(), any())
        } returns Optional.empty()
    }

    /**
     * The maker's node checks the offer of the take against the one it published, and the core's
     * version check reads the offer's app version. A copy rebuilt from the app's model would carry
     * this app's versions instead, so the take must use the published offer itself.
     */
    @Test
    fun `takeOffer hands the maker's published offer to the core`() =
        runTest {
            givenTakeOfferPrerequisites()
            val (offerVO, publishedOffer) = givenOfferInOfferbook()
            val offerSlot = slot<BisqEasyOffer>()
            every {
                bisqEasyTradeService.takerCreatesProtocol(any(), capture(offerSlot), any(), any(), any(), any(), any(), any())
            } throws IllegalStateException("stop after the offer was handed over")
            mockkObject(Mappings.MonetaryMapping)
            try {
                every { Mappings.MonetaryMapping.toBisq2Model(any()) } returns mockk<Monetary>(relaxed = true)

                facade.takeOffer(
                    offerVO,
                    mockk<MonetaryVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    "MAIN_CHAIN",
                    "SEPA",
                    MutableStateFlow(null),
                    MutableStateFlow(null),
                )

                assertSame(publishedOffer, offerSlot.captured)
            } finally {
                unmockkObject(Mappings.MonetaryMapping)
            }
        }

    @Test
    fun `takeOffer of an offer which left the offerbook stops before the core`() =
        runTest {
            givenTakeOfferPrerequisites()
            givenOfferInOfferbook(offerId = "another-offer")
            val removedOffer = mockk<BisqEasyOfferVO>(relaxed = true) { every { id } returns "removed-offer" }
            val errorMessage = MutableStateFlow<String?>(null)

            val result =
                facade.takeOffer(
                    removedOffer,
                    mockk<MonetaryVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    "MAIN_CHAIN",
                    "SEPA",
                    MutableStateFlow(null),
                    errorMessage,
                )

            assertTrue(result.isFailure)
            assertEquals("mobile.bisqEasy.takeOffer.offerNoLongerAvailable".i18n(), errorMessage.value)
            verify(exactly = 0) {
                bisqEasyTradeService.takerCreatesProtocol(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

    /** Runs takeOffer with the core refusing the take in takerCreatesProtocol; returns the user-facing message. */
    private suspend fun takeOfferRefusedWith(refusal: Exception): String? {
        givenTakeOfferPrerequisites()
        every {
            bisqEasyTradeService.takerCreatesProtocol(any(), any(), any(), any(), any(), any(), any(), any())
        } throws refusal
        val (offerVO, _) = givenOfferInOfferbook()
        mockkObject(Mappings.MonetaryMapping)
        try {
            every { Mappings.MonetaryMapping.toBisq2Model(any()) } returns mockk<Monetary>(relaxed = true)
            val errorMessage = MutableStateFlow<String?>(null)

            val result =
                facade.takeOffer(
                    offerVO,
                    mockk<MonetaryVO>(relaxed = true),
                    mockk<MonetaryVO>(relaxed = true),
                    "MAIN_CHAIN",
                    "SEPA",
                    MutableStateFlow(null),
                    errorMessage,
                )

            assertTrue(result.isFailure)
            return errorMessage.value
        } finally {
            unmockkObject(Mappings.MonetaryMapping)
        }
    }

    @Test
    fun `takeOffer refused because the maker version is below the minimum asks to take another offer`() =
        runTest {
            val message = takeOfferRefusedWith(TradeRestrictedException.makerVersionBelowMinRequired("2.1.14"))

            assertEquals("mobile.bisqEasy.takeOffer.makerVersionBelowMinRequired".i18n("2.1.14"), message)
        }

    @Test
    fun `takeOffer refused because the own version is below the minimum asks to update the app`() =
        runTest {
            val message = takeOfferRefusedWith(TradeRestrictedException.minVersionRequired("2.1.14"))

            assertEquals("mobile.bisqEasy.takeOffer.minVersionRequired.node".i18n("2.1.14"), message)
        }

    @Test
    fun `takeOffer refused because trading is halted shows the halted message`() =
        runTest {
            val message = takeOfferRefusedWith(TradeRestrictedException.haltTrading())

            assertEquals("mobile.bisqEasy.takeOffer.tradingHalted".i18n(), message)
        }

    @Test
    fun `takeOffer restriction wrapped in another exception is still classified by its type`() =
        runTest {
            val message =
                takeOfferRefusedWith(
                    IllegalStateException("wrapper", TradeRestrictedException.makerVersionBelowMinRequired("2.1.14")),
                )

            assertEquals("mobile.bisqEasy.takeOffer.makerVersionBelowMinRequired".i18n("2.1.14"), message)
        }

    /**
     * A failure raised inside doTakeOffer is logged twice, once by doTakeOffer and once by takeOffer.
     * Neither line may carry the exception message, which on this path is the protocol text.
     */
    @Test
    fun `a failure inside doTakeOffer is logged twice without the exception message`() =
        runTest {
            I18nSupport.initialize("en")
            val userService = mockk<UserService>(relaxed = true)
            every { userService.bannedUserService.isUserProfileBanned(any<UserProfile>()) } returns true
            every { applicationService.userService } returns userService
            val (offerVO, _) = givenOfferInOfferbook()
            mockkObject(Mappings.MonetaryMapping)
            try {
                every { Mappings.MonetaryMapping.toBisq2Model(any()) } returns mockk<Monetary>(relaxed = true)
                captureErrorLogs()
                val errorMessage = MutableStateFlow<String?>(null)

                val result =
                    facade.takeOffer(
                        offerVO,
                        mockk<MonetaryVO>(relaxed = true),
                        mockk<MonetaryVO>(relaxed = true),
                        "MAIN_CHAIN",
                        "SEPA",
                        MutableStateFlow(null),
                        errorMessage,
                    )

                assertTrue(result.isFailure)
                assertEquals(2, capturedErrors.size)
                assertTrue(capturedErrors[0].first.startsWith("doTakeOffer failed: IllegalStateException"), capturedErrors[0].first)
                assertTrue(capturedErrors[1].first.startsWith("Failed to take offer: IllegalStateException"), capturedErrors[1].first)
                capturedErrors.forEach { (message, throwable) ->
                    assertFalse(message.contains("banned", ignoreCase = true), message)
                    assertNull(throwable)
                }
                assertEquals("mobile.bisqEasy.takeOffer.userBanned".i18n(), errorMessage.value)
            } finally {
                unmockkObject(Mappings.MonetaryMapping)
            }
        }

    @Test
    fun `open trades the node already holds are complete and marked synced once activate returns`() =
        runTest {
            // A trade the node holds before the facade binds, as on every restart.
            trades.add(givenOpenTrade(TRADE_ID))
            assertFalse(facade.openTradesSynced.value, "Nothing has been delivered yet")

            facade.activate()

            assertEquals(listOf(TRADE_ID), facade.openTradeItems.value.map { it.tradeId })
            assertTrue(facade.openTradesSynced.value)

            // runCurrent, not advanceUntilIdle: activation starts the analytics out-of-sync recheck
            // ticker, and an infinite ticker keeps the virtual-time scheduler busy forever.
            runCurrent()
            facade.deactivate()
            assertFalse(facade.openTradesSynced.value)
        }

    @Test
    fun `account data banned check delegates to the node's banned set`() =
        runTest {
            every { bisqEasyService.isAccountDataBanned(BANNED_DATA) } returns true
            every { bisqEasyService.isAccountDataBanned(CLEAN_DATA) } returns false

            assertTrue(facade.isAccountDataBanned(BANNED_DATA))
            assertFalse(facade.isAccountDataBanned(CLEAN_DATA))
        }

    @Test
    fun `account data banned check runs off the caller's thread`() =
        runTest {
            var checkThread: Thread? = null
            every { bisqEasyService.isAccountDataBanned(BANNED_DATA) } answers {
                checkThread = Thread.currentThread()
                true
            }

            facade.isAccountDataBanned(BANNED_DATA)

            assertNotNull(checkThread)
            assertNotEquals(Thread.currentThread(), checkThread)
        }

    @Test
    fun `banned account data cancel is local and sends no trade log message`() =
        runTest {
            val trade = givenOpenTrade(TRADE_ID)
            trades.add(trade)
            every { bisqEasyTradeService.findTrade(TRADE_ID) } returns Optional.of(trade)
            // A known stall age, so the UNKNOWN bucket below is the facade's choice and not a missing clock.
            coEvery { stallClockRepository.fetch() } returns
                TradeStallClockMap(mapOf(TRADE_ID to TradeStallClockEntry(BisqEasyTradeState.INIT.name, transitionAtMs = 0)))
            facade.activate()
            try {
                runCurrent()
                facade.selectOpenTrade(TRADE_ID)

                val result = facade.cancelTradeForBannedAccountData()

                assertTrue(result.isSuccess)
                verify(exactly = 1) { bisqEasyTradeService.cancelTrade(trade) }
                verify(exactly = 0) { channelService.sendTradeLogMessage(any(), any()) }
                verify { analyticsService.track(Trade.Cancelled(Trade.InterruptReason.BANNED_ACCOUNT_DATA, Trade.StallBucket.UNKNOWN)) }
                runCurrent()
            } finally {
                // Always: a failed assertion would otherwise leave the facade active for the next test.
                facade.deactivate()
            }
        }

    @Test
    fun `rejected bitcoin payment data is not posted to the trade chat`() =
        runTest {
            val trade = givenSelectedTrade()
            every { bisqEasyTradeService.buyerSendBitcoinPaymentData(trade, INVOICE) } throws
                IllegalArgumentException("The bitcoin payment data must not request another amount than the trade amount")
            try {
                val result = facade.buyerSendBitcoinPaymentData(INVOICE)

                assertTrue(result.isFailure)
                verify(exactly = 0) { channelService.sendTradeLogMessage(any(), any()) }
            } finally {
                facade.deactivate()
            }
        }

    @Test
    fun `accepted bitcoin payment data is posted to the trade chat after the trade service took it`() =
        runTest {
            val trade = givenSelectedTrade()
            try {
                val result = facade.buyerSendBitcoinPaymentData(INVOICE)

                assertTrue(result.isSuccess)
                verifyOrder {
                    bisqEasyTradeService.buyerSendBitcoinPaymentData(trade, INVOICE)
                    channelService.sendTradeLogMessage(any(), any())
                }
            } finally {
                facade.deactivate()
            }
        }

    @Test
    fun `no trade action posts a trade log message when the trade service rejects it`() =
        runTest {
            val trade = givenSelectedTrade()
            val rejection = IllegalStateException("rejected")
            every { bisqEasyTradeService.sellerSendsPaymentAccount(trade, any()) } throws rejection
            every { bisqEasyTradeService.buyerSendBitcoinPaymentData(trade, any()) } throws rejection
            every { bisqEasyTradeService.sellerConfirmFiatReceipt(trade) } throws rejection
            every { bisqEasyTradeService.buyerConfirmFiatSent(trade) } throws rejection
            every { bisqEasyTradeService.sellerConfirmBtcSent(trade, any()) } throws rejection
            every { bisqEasyTradeService.btcConfirmed(trade) } throws rejection
            every { bisqEasyTradeService.rejectTrade(trade) } throws rejection
            every { bisqEasyTradeService.cancelTrade(trade) } throws rejection
            try {
                val results =
                    listOf(
                        facade.sellerSendsPaymentAccount("IBAN"),
                        facade.buyerSendBitcoinPaymentData(INVOICE),
                        facade.sellerConfirmFiatReceipt(),
                        facade.buyerConfirmFiatSent(),
                        facade.sellerConfirmBtcSent("txid"),
                        facade.btcConfirmed(),
                        facade.rejectTrade(Trade.InterruptReason.PEER_UNRESPONSIVE),
                        facade.cancelTrade(Trade.InterruptReason.PEER_UNRESPONSIVE),
                    )

                assertTrue(results.all { it.isFailure })
                verify(exactly = 0) { channelService.sendTradeLogMessage(any(), any()) }
            } finally {
                facade.deactivate()
            }
        }

    @Test
    fun `a reject or cancel the trade service took succeeds when its trade log message fails`() =
        runTest {
            val trade = givenSelectedTrade()
            every { channelService.sendTradeLogMessage(any(), any()) } returns
                CompletableFuture.failedFuture(IllegalStateException("send failed"))
            try {
                val rejected = facade.rejectTrade(Trade.InterruptReason.PEER_UNRESPONSIVE)
                val cancelled = facade.cancelTrade(Trade.InterruptReason.PEER_UNRESPONSIVE)

                assertTrue(rejected.isSuccess)
                assertTrue(cancelled.isSuccess)
                verifyOrder {
                    bisqEasyTradeService.rejectTrade(trade)
                    channelService.sendTradeLogMessage(any(), any())
                    bisqEasyTradeService.cancelTrade(trade)
                    channelService.sendTradeLogMessage(any(), any())
                }
            } finally {
                facade.deactivate()
            }
        }

    /** An active facade with a Lightning trade selected, whose own side is the buyer. */
    private suspend fun TestScope.givenSelectedTrade(): BisqEasyTrade {
        val trade = givenOpenTrade(TRADE_ID)
        every { trade.isBuyer } returns true
        val paymentMethod = mockk<BitcoinPaymentMethod> { every { paymentRail } returns BitcoinPaymentRail.LN }
        val paymentMethodSpec = mockk<BitcoinPaymentMethodSpec> { every { this@mockk.paymentMethod } returns paymentMethod }
        val contract = mockk<BisqEasyContract>(relaxed = true) { every { baseSidePaymentMethodSpec } returns paymentMethodSpec }
        every { trade.contract } returns contract
        trades.add(trade)
        every { bisqEasyTradeService.findTrade(TRADE_ID) } returns Optional.of(trade)
        // A sent message, so an awaited send completes instead of hanging on the relaxed mock's future.
        every { channelService.sendTradeLogMessage(any(), any()) } returns CompletableFuture.completedFuture(null)
        facade.activate()
        runCurrent()
        facade.selectOpenTrade(TRADE_ID)
        return trade
    }

    @Test
    fun `banned account data cancel saves the reason for the trade`() =
        runTest {
            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)

                coVerify(exactly = 1) { cancelReasonRepository.setReason(TRADE_ID, TradeCancelReason.BANNED_ACCOUNT_DATA) }
            }
        }

    @Test
    fun `banned account data cancel saves the reason before sending the cancel and keeps it`() =
        runTest {
            withSelectedTrade { trade ->
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)

                coVerifyOrder {
                    cancelReasonRepository.setReason(TRADE_ID, TradeCancelReason.BANNED_ACCOUNT_DATA)
                    bisqEasyTradeService.cancelTrade(trade)
                }
                coVerify(exactly = 0) { cancelReasonRepository.clearId(any()) }
            }
        }

    @Test
    fun `a failed banned account data cancel removes the reason it saved`() =
        runTest {
            withSelectedTrade { trade ->
                every { bisqEasyTradeService.cancelTrade(trade) } throws IllegalStateException("protocol error")

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
            coEvery { cancelReasonRepository.setReason(any(), any()) } throws IllegalStateException("disk full")

            withSelectedTrade {
                assertTrue(facade.cancelTradeForBannedAccountData().isSuccess)
            }
        }

    @Test
    fun `a user cancel saves no reason`() =
        runTest {
            every { channelService.sendTradeLogMessage(any(), any()) } returns CompletableFuture.completedFuture(mockk(relaxed = true))

            withSelectedTrade {
                assertTrue(facade.cancelTrade(Trade.InterruptReason.PRICE_MOVED).isSuccess)

                coVerify(exactly = 0) { cancelReasonRepository.setReason(any(), any()) }
            }
        }

    /** Runs [block] on an activated facade whose selected trade is [TRADE_ID]. */
    private suspend fun TestScope.withSelectedTrade(block: suspend (BisqEasyTrade) -> Unit) {
        val trade = givenOpenTrade(TRADE_ID)
        trades.add(trade)
        every { bisqEasyTradeService.findTrade(TRADE_ID) } returns Optional.of(trade)
        facade.activate()
        try {
            runCurrent()
            facade.selectOpenTrade(TRADE_ID)
            block(trade)
            runCurrent()
        } finally {
            // Always: a failed assertion would otherwise leave the facade active for the next test.
            facade.deactivate()
        }
    }

    private fun givenOpenTrade(tradeId: String): BisqEasyTrade {
        val trade = mockk<BisqEasyTrade>(relaxed = true)
        every { trade.id } returns tradeId
        every { trade.tradeState } returns BisqEasyTradeState.INIT
        val channel = mockk<Bisq2BisqEasyOpenTradeChannel>(relaxed = true)
        every { channel.tradeId } returns tradeId
        every { channelService.findChannelByTradeId(tradeId) } returns Optional.of(channel)
        val item = mockk<TradeItemPresentationModel>(relaxed = true)
        every { item.tradeId } returns tradeId
        every { item.bisqEasyTradeModel.tradeState } returns MutableStateFlow(BisqEasyTradeStateEnum.INIT)
        every { TradeItemPresentationModelFactory.create(trade, channel, any(), any()) } returns item
        return trade
    }

    private companion object {
        const val TRADE_ID = "trade-1"
        const val INVOICE = "lnbc25u1pexample"
        const val BANNED_DATA = "IBAN DE89 3704 0044 0532 0130 00"
        const val CLEAN_DATA = "IBAN DE12 5001 0517 0648 4898 90"
    }
}
