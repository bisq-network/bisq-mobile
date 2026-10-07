package network.bisq.mobile.node.common.domain.service.trades

import bisq.chat.ChatService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.common.observable.collection.ObservableSet
import bisq.trade.TradeService
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeService
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.TradeItemPresentationModelFactory
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    private lateinit var facade: NodeTradesServiceFacade

    override fun onSetup() {
        val bisqEasyTradeService = mockk<BisqEasyTradeService>(relaxed = true)
        every { bisqEasyTradeService.trades } returns trades
        val tradeService = mockk<TradeService>()
        every { tradeService.bisqEasyTradeService } returns bisqEasyTradeService
        val chatService = mockk<ChatService>()
        every { chatService.bisqEasyOpenTradeChannelService } returns channelService

        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.tradeService } returns tradeService
        every { applicationService.chatService } returns chatService
        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        mockkObject(TradeItemPresentationModelFactory)
        facade = NodeTradesServiceFacade(provider, mockk(relaxed = true), mockk(relaxed = true))
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
            mockkObject(Mappings.BisqEasyOfferMapping)
            try {
                every { Mappings.BisqEasyOfferMapping.toBisq2Model(any()) } throws IllegalStateException(peerText)
                captureErrorLogs()
                val errorMessage = MutableStateFlow<String?>(null)

                val result =
                    facade.takeOffer(
                        mockk<BisqEasyOfferVO>(relaxed = true),
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
                unmockkObject(Mappings.BisqEasyOfferMapping)
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
    }
}
