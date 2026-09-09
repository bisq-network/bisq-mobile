package network.bisq.mobile.presentation.common.service

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.model.NotificationBuilder
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers issue #1829: on a cold start the trades arrive before their chat history, so a chat baseline
 * taken when the trade appears reads 0 and the history landing later notified once per trade.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradesNotificationServiceChatSyncTest : PresentationKoinTestBase() {
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val appForegroundController: ForegroundDetector = mockk(relaxed = true)

    private val openTrades = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    private val chatMessagesSynced = MutableStateFlow(false)
    private val isForeground = MutableStateFlow(true)

    private var notifyCount = 0

    private lateinit var foregroundServiceController: CollectingForegroundServiceController
    private lateinit var service: OpenTradesNotificationService

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        every { tradesServiceFacade.openTradeItems } returns openTrades
        every { tradeChatMessagesServiceFacade.chatMessagesSynced } returns chatMessagesSynced
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { appForegroundController.isForeground } returns isForeground
        every { notificationController.notify(any<NotificationBuilder.() -> Unit>()) } answers { notifyCount++ }

        foregroundServiceController = CollectingForegroundServiceController(CoroutineScope(SupervisorJob() + testDispatcher))
        service =
            OpenTradesNotificationService(
                notificationController = notificationController,
                foregroundServiceController = foregroundServiceController,
                tradesServiceFacade = tradesServiceFacade,
                tradeChatMessagesServiceFacade = tradeChatMessagesServiceFacade,
                userProfileServiceFacade = userProfileServiceFacade,
                appForegroundController = appForegroundController,
                dispatcher = testDispatcher,
            )
    }

    @Test
    fun `chat history that lands after a cold start does not notify`() =
        runTest {
            // Given - the service observes while the trades are still on their way
            val first = tradeWithChat("trade-1")
            val second = tradeWithChat("trade-2")
            goBackground()

            // When - the trades arrive with empty channels, then the history snapshot lands
            openTrades.value = listOf(first.trade, second.trade)
            advanceUntilIdle()
            first.messages.value = peerMessages("trade-1", 3)
            second.messages.value = peerMessages("trade-2", 1)
            chatMessagesSynced.value = true
            advanceUntilIdle()

            // Then
            assertEquals(0, notifyCount, "everything present at the first sync is history")
        }

    @Test
    fun `a peer message arriving after the first sync notifies once for its trade`() =
        runTest {
            val first = tradeWithChat("trade-1")
            val second = tradeWithChat("trade-2")
            goBackground()
            openTrades.value = listOf(first.trade, second.trade)
            first.messages.value = peerMessages("trade-1", 3)
            chatMessagesSynced.value = true
            advanceUntilIdle()

            first.messages.value = peerMessages("trade-1", 4)
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    @Test
    fun `a sync that fails first does not disarm notifications once it later succeeds`() =
        runTest {
            val trade = tradeWithChat("trade-1")
            goBackground()
            openTrades.value = listOf(trade.trade)
            advanceUntilIdle()

            // When - the first sync never comes, then a reconnect delivers history and a newer message
            chatMessagesSynced.value = false
            advanceUntilIdle()
            trade.messages.value = peerMessages("trade-1", 2)
            chatMessagesSynced.value = true
            advanceUntilIdle()
            assertEquals(0, notifyCount, "the late history is still history")

            trade.messages.value = peerMessages("trade-1", 3)
            advanceUntilIdle()

            assertEquals(1, notifyCount)
        }

    @Test
    fun `a message that arrived while sync was down notifies once sync is back`() =
        runTest {
            val trade = tradeWithChat("trade-1")
            goBackground()
            openTrades.value = listOf(trade.trade)
            trade.messages.value = peerMessages("trade-1", 2)
            chatMessagesSynced.value = true
            advanceUntilIdle()

            chatMessagesSynced.value = false
            advanceUntilIdle()
            trade.messages.value = peerMessages("trade-1", 3)
            chatMessagesSynced.value = true
            advanceUntilIdle()

            assertEquals(1, notifyCount, "the count survives the gap, so the message is new against it")
        }

    @Test
    fun `own messages in the history do not count`() =
        runTest {
            val trade = tradeWithChat("trade-1")
            goBackground()
            openTrades.value = listOf(trade.trade)
            chatMessagesSynced.value = true
            advanceUntilIdle()

            trade.messages.value = setOf(createMockBisqEasyOpenTradeMessage(tradeId = "trade-1", senderUserProfile = ME, myUserProfile = ME))
            advanceUntilIdle()

            assertEquals(0, notifyCount)
        }

    private class TradeWithChat(
        val trade: TradeItemPresentationModel,
        val messages: MutableStateFlow<Set<BisqEasyOpenTradeMessage>>,
    )

    private fun tradeWithChat(tradeId: String): TradeWithChat {
        val messages = MutableStateFlow<Set<BisqEasyOpenTradeMessage>>(emptySet())
        val channel = mockk<BisqEasyOpenTradeChannel>(relaxed = true)
        every { channel.chatMessages } returns messages

        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.takeOfferDate } returns 0L
        every { tradeModel.tradeState } returns MutableStateFlow(BisqEasyTradeStateEnum.INIT)
        every { tradeModel.paymentAccountData } returns MutableStateFlow<String?>(null)
        every { tradeModel.bitcoinPaymentData } returns MutableStateFlow<String?>(null)

        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.tradeId } returns tradeId
        every { trade.shortTradeId } returns tradeId
        every { trade.peersUserName } returns "peer"
        every { trade.bisqEasyTradeModel } returns tradeModel
        every { trade.bisqEasyOpenTradeChannelModel } returns channel
        return TradeWithChat(trade, messages)
    }

    private fun peerMessages(
        tradeId: String,
        count: Int,
    ): Set<BisqEasyOpenTradeMessage> =
        (1..count)
            .map { createMockBisqEasyOpenTradeMessage(id = "$tradeId-$it", tradeId = tradeId, senderUserProfile = PEER, myUserProfile = ME) }
            .toSet()

    private suspend fun TestScope.goBackground() {
        isForeground.value = false
        advanceUntilIdle()
    }

    /** Collects each registered flow on [scope], the way the real Android controller does. */
    private class CollectingForegroundServiceController(
        private val scope: CoroutineScope,
    ) : ForegroundServiceController {
        private val jobs = mutableMapOf<Flow<*>, Job>()

        override fun startService() {}

        override fun stopService() {}

        override fun refreshNotification() {}

        override fun <T> registerObserver(
            flow: Flow<T>,
            onStateChange: suspend (T) -> Unit,
        ) {
            jobs[flow] = scope.launch { flow.collect { onStateChange(it) } }
        }

        override fun unregisterObserver(flow: Flow<*>) {
            jobs.remove(flow)?.cancel()
        }

        override fun unregisterObservers() {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
        }

        override fun isServiceRunning(): Boolean = true

        override fun dispose() {
            scope.cancel()
        }
    }

    private companion object {
        val ME = createMockUserProfile("me")
        val PEER = createMockUserProfile("peer")
    }
}
