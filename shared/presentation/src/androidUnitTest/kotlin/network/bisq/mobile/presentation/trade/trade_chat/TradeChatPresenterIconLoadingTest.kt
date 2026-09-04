package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that TradeChatPresenter loads user profile icons off the main thread.
 * Verifies the fix for iOS CA Fence hang (issue #1225).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterIconLoadingTest : PlatformPresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `initialize loads user profile icons for chat messages`() =
        runBlocking {
            val userProfile1 = createMockUserProfile("sender1")
            val userProfile2 = createMockUserProfile("sender2")
            val myUserProfile = createMockUserProfile("myUser")

            val model1 =
                createMockBisqEasyOpenTradeMessage(
                    id = "msg1",
                    text = "hello",
                    date = 1000L,
                    senderUserProfile = userProfile1,
                    myUserProfile = myUserProfile,
                    tradeId = "trade1",
                )

            val model2 =
                createMockBisqEasyOpenTradeMessage(
                    id = "msg2",
                    text = "world",
                    date = 2000L,
                    senderUserProfile = userProfile2,
                    myUserProfile = myUserProfile,
                    tradeId = "trade1",
                )

            val chatMessagesFlow = MutableStateFlow(setOf(model1, model2))
            val channelModel = mockk<BisqEasyOpenTradeChannel>()
            every { channelModel.chatMessages } returns chatMessagesFlow

            val trade = mockk<TradeItemPresentationModel>()
            every { trade.tradeId } returns "trade1"
            every { trade.shortTradeId } returns "t1"
            every { trade.bisqEasyOpenTradeChannelModel } returns channelModel

            val tradesServiceFacade = mockk<TradesServiceFacade>(relaxed = true)
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(trade)
            // The presenter retries the lookup on every open-trades update; the stubbed selectedTrade
            // resolves on the replayed current value, so the list content itself does not matter here.
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())
            every { tradesServiceFacade.openTradesSynced } returns MutableStateFlow(true)

            val mockImage = mockk<PlatformImage>()
            val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
            coEvery { userProfileServiceFacade.getUserProfileIcon(any()) } returns mockImage
            every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())

            val mainPresenter = MainPresenterTestFactory.create()

            val tradeChatMessagesServiceFacade = mockk<TradeChatMessagesServiceFacade>(relaxed = true)
            every { tradeChatMessagesServiceFacade.chatMessagesSynced } returns MutableStateFlow(true)

            val presenter =
                TradeChatPresenter(
                    mainPresenter = mainPresenter,
                    tradesServiceFacade = tradesServiceFacade,
                    tradeChatMessagesServiceFacade = tradeChatMessagesServiceFacade,
                    settingsRepository = mockk<SettingsRepository>(relaxed = true),
                    tradeReadStateRepository = mockk<TradeReadStateRepository>(relaxed = true),
                    userProfileServiceFacade = userProfileServiceFacade,
                    notificationController = mockk<NotificationController>(relaxed = true),
                    messageDeliveryServiceFacade = mockk<MessageDeliveryServiceFacade>(relaxed = true),
                )

            presenter.initialize("trade1")
            // Allow coroutines to complete (withContext(Dispatchers.IO) needs real dispatch)
            delay(300)

            val icons = presenter.userProfileIconByProfileId.first()
            assertEquals(2, icons.size, "Should have loaded icons for both senders")
            assertEquals(mockImage, icons[userProfile1.id])
            assertEquals(mockImage, icons[userProfile2.id])
        }
}
