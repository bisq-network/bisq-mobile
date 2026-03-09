package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that TradeChatPresenter loads user profile icons off the main thread.
 * Verifies the fix for iOS CA Fence hang (issue #1225).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterIconLoadingTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single { NoopNavigationManager() as network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager }
                    single { GlobalUiManager() }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }

    @Test
    fun `initialize loads user profile icons for chat messages`() =
        runBlocking {
            val userProfile1 = createMockUserProfile("sender1")
            val userProfile2 = createMockUserProfile("sender2")
            val myUserProfile = createMockUserProfile("myUser")

            val dto1 = mockk<BisqEasyOpenTradeMessageDto>()
            every { dto1.chatMessageType } returns ChatMessageTypeEnum.TEXT
            every { dto1.senderUserProfile } returns userProfile1
            every { dto1.messageId } returns "msg1"
            every { dto1.text } returns "hello"
            every { dto1.citation } returns null
            every { dto1.date } returns 1000L
            every { dto1.tradeId } returns "trade1"
            every { dto1.mediator } returns null
            every { dto1.bisqEasyOffer } returns null
            every { dto1.citationAuthorUserProfile } returns null
            val model1 = BisqEasyOpenTradeMessageModel(dto1, myUserProfile, emptyList())

            val dto2 = mockk<BisqEasyOpenTradeMessageDto>()
            every { dto2.chatMessageType } returns ChatMessageTypeEnum.TEXT
            every { dto2.senderUserProfile } returns userProfile2
            every { dto2.messageId } returns "msg2"
            every { dto2.text } returns "world"
            every { dto2.citation } returns null
            every { dto2.date } returns 2000L
            every { dto2.tradeId } returns "trade1"
            every { dto2.mediator } returns null
            every { dto2.bisqEasyOffer } returns null
            every { dto2.citationAuthorUserProfile } returns null
            val model2 = BisqEasyOpenTradeMessageModel(dto2, myUserProfile, emptyList())

            val chatMessagesFlow = MutableStateFlow(setOf(model1, model2))
            val channelModel = mockk<BisqEasyOpenTradeChannelModel>()
            every { channelModel.chatMessages } returns chatMessagesFlow

            val trade = mockk<TradeItemPresentationModel>()
            every { trade.tradeId } returns "trade1"
            every { trade.shortTradeId } returns "t1"
            every { trade.bisqEasyOpenTradeChannelModel } returns channelModel

            val tradesServiceFacade = mockk<TradesServiceFacade>(relaxed = true)
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(trade)

            val mockImage = mockk<PlatformImage>()
            val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
            coEvery { userProfileServiceFacade.getUserProfileIcon(any()) } returns mockImage
            every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())

            val mainPresenter = MainPresenterTestFactory.create()

            val presenter =
                TradeChatPresenter(
                    mainPresenter = mainPresenter,
                    tradesServiceFacade = tradesServiceFacade,
                    tradeChatMessagesServiceFacade = mockk(relaxed = true),
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
