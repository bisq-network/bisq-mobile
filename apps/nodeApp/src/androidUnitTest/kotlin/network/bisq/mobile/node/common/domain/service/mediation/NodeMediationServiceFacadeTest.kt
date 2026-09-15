package network.bisq.mobile.node.common.domain.service.mediation

import bisq.chat.ChatService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.support.SupportService
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.offers.MediatorNotAvailableException
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Pins the mediation-request persistence contract to desktop's OpenTradesUtils.requestMediation:
 * the in-mediation flag must hit the channel store immediately, not ride along with whichever
 * persist happens next — an app kill between the request and that incidental persist would
 * otherwise revert the flag, dropping the mediator from CC on messages sent after restart.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeMediationServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private companion object {
        const val TRADE_ID = "trade-1"
    }

    private val channelService: BisqEasyOpenTradeChannelService = mockk(relaxed = true)
    private val mediationRequestService: BisqEasyMediationRequestService = mockk(relaxed = true)
    private lateinit var facade: NodeMediationServiceFacade

    override fun onSetup() {
        val chatService = mockk<ChatService>()
        every { chatService.bisqEasyOpenTradeChannelService } returns channelService
        val supportService = mockk<SupportService>()
        every { supportService.bisqEasyMediationRequestService } returns mediationRequestService

        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.chatService } returns chatService
        every { applicationService.supportService } returns supportService
        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        // The contract mapping needs a fully populated VO tree; this test is about the
        // persistence contract, not the mapping, which has its own coverage.
        mockkObject(Mappings.BisqEasyContractMapping)
        every { Mappings.BisqEasyContractMapping.toBisq2Model(any()) } returns mockk()

        facade = NodeMediationServiceFacade(provider)
    }

    override fun onTearDown() {
        try {
            unmockkObject(Mappings.BisqEasyContractMapping)
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `reporting to the mediator persists the channel store between flagging and requesting`() =
        runTest {
            val channel = channelWithMediator()

            val result = facade.reportToMediator(tradeItem())

            assertTrue(result.isSuccess)
            // Desktop's exact order: flag, persist, then request — persisting after the request
            // would reopen a (smaller) kill window while the request is in flight.
            verifyOrder {
                channel.setIsInMediation(true)
                channelService.persist()
                mediationRequestService.requestMediation(channel, any())
            }
        }

    @Test
    fun `a missing mediator fails without flagging or persisting`() =
        runTest {
            val channel = mockk<BisqEasyOpenTradeChannel>(relaxed = true)
            every { channel.mediator } returns Optional.empty()
            every { channelService.findChannelByTradeId(TRADE_ID) } returns Optional.of(channel)

            val result = facade.reportToMediator(tradeItem())

            assertIs<MediatorNotAvailableException>(result.exceptionOrNull())
            verify(exactly = 0) { channel.setIsInMediation(any()) }
            verify(exactly = 0) { channelService.persist() }
        }

    @Test
    fun `an unknown trade id fails without touching the store`() =
        runTest {
            every { channelService.findChannelByTradeId(TRADE_ID) } returns Optional.empty()

            val result = facade.reportToMediator(tradeItem())

            assertTrue(result.isFailure)
            verify(exactly = 0) { channelService.persist() }
        }

    private fun channelWithMediator(): BisqEasyOpenTradeChannel {
        val channel = mockk<BisqEasyOpenTradeChannel>(relaxed = true)
        every { channel.mediator } returns Optional.of(mockk(relaxed = true))
        every { channel.myUserIdentity } returns mockk(relaxed = true) { every { userName } returns "alice" }
        every { channelService.findChannelByTradeId(TRADE_ID) } returns Optional.of(channel)
        every { channelService.sendTradeLogMessage(any(), channel) } returns CompletableFuture.completedFuture(mockk(relaxed = true))
        return channel
    }

    private fun tradeItem(): TradeItemPresentationModel = mockk(relaxed = true) { every { tradeId } returns TRADE_ID }
}
