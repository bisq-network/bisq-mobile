package network.bisq.mobile.node.common.domain.service.trades

import bisq.bisq_easy.BisqEasyService
import bisq.chat.ChatService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.observable.collection.ObservableSet
import bisq.trade.TradeService
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeService
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.analytics.AnalyticsEvent.Trade
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeStallClockRepository
import network.bisq.mobile.node.common.domain.mapping.TradeItemPresentationModelFactory
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
    private val bisqEasyTradeService: BisqEasyTradeService = mockk(relaxed = true)
    private val bisqEasyService: BisqEasyService = mockk()
    private val analyticsService: AnalyticsService = mockk(relaxed = true)
    private val stallClockRepository: TradeStallClockRepository = mockk(relaxed = true)
    private lateinit var facade: NodeTradesServiceFacade

    override fun onSetup() {
        every { bisqEasyTradeService.trades } returns trades
        val tradeService = mockk<TradeService>()
        every { tradeService.bisqEasyTradeService } returns bisqEasyTradeService
        val chatService = mockk<ChatService>()
        every { chatService.bisqEasyOpenTradeChannelService } returns channelService

        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.tradeService } returns tradeService
        every { applicationService.chatService } returns chatService
        every { applicationService.bisqEasyService } returns bisqEasyService
        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        mockkObject(TradeItemPresentationModelFactory)
        facade = NodeTradesServiceFacade(provider, analyticsService, stallClockRepository)
    }

    override fun onTearDown() {
        try {
            unmockkObject(TradeItemPresentationModelFactory)
        } finally {
            super.onTearDown()
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
            runCurrent()
            facade.selectOpenTrade(TRADE_ID)

            val result = facade.cancelTradeForBannedAccountData()

            assertTrue(result.isSuccess)
            verify(exactly = 1) { bisqEasyTradeService.cancelTrade(trade) }
            verify(exactly = 0) { channelService.sendTradeLogMessage(any(), any()) }
            verify { analyticsService.track(Trade.Cancelled(Trade.InterruptReason.BANNED_ACCOUNT_DATA, Trade.StallBucket.UNKNOWN)) }

            runCurrent()
            facade.deactivate()
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
        every { TradeItemPresentationModelFactory.create(trade, channel, any(), any()) } returns item
        return trade
    }

    private companion object {
        const val TRADE_ID = "trade-1"
        const val BANNED_DATA = "IBAN DE89 3704 0044 0532 0130 00"
        const val CLEAN_DATA = "IBAN DE12 5001 0517 0648 4898 90"
    }
}
