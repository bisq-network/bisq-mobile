package network.bisq.mobile.presentation

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.profile.userProfileDemoObj
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the unread badge logic in MainPresenter.
 * Eventually we should refactor this responsibility to a separate service that
 * the presenter can interact with
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainPresenterUnreadBadgeTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unread badge count reflects the total unread chat messages exactly`() = runTest {
        // Mock top-level android-specific function called from MainPresenter.init
        mockkStatic("network.bisq.mobile.presentation.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        // Mock dependencies
        val tradesServiceFacade = mockk<TradesServiceFacade>()
        val tradeReadStateRepository = mockk<TradeReadStateRepository>()
        val userProfileServiceFacade = mockk<UserProfileServiceFacade>()
        val openTradesNotificationService = mockk<OpenTradesNotificationService>()
        val settingsService = mockk<SettingsServiceFacade>()
        every { settingsService.languageCode } returns MutableStateFlow("en")
        every { settingsService.useAnimations } returns MutableStateFlow(false)
        val urlLauncher = mockk<UrlLauncher>(relaxed = true)

        // Mock ignored user IDs
        val ignoredUserIdsFlow = MutableStateFlow(setOf("ignoredUser1"))
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredUserIdsFlow

        // Mock read states
        val readStatesFlow = MutableStateFlow(TradeReadStateMap(mapOf("trade1" to 1, "trade2" to 0)))
        every { tradeReadStateRepository.data } returns readStatesFlow

        // Mock myUserProfile for models
        val myUserProfile = createMockUserProfile("myUser")

        // Mock DTOs and create models for trade1
        val dto1 = mockk<BisqEasyOpenTradeMessageDto>()
        every { dto1.chatMessageType } returns ChatMessageTypeEnum.TEXT
        every { dto1.senderUserProfile } returns createMockUserProfile("User1")
        every { dto1.messageId } returns "msg1"
        every { dto1.text } returns null
        every { dto1.citation } returns null
        every { dto1.date } returns 0L
        every { dto1.tradeId } returns "trade1"
        every { dto1.mediator } returns null
        every { dto1.bisqEasyOffer } returns null
        every { dto1.citationAuthorUserProfile } returns null
        val model1 = BisqEasyOpenTradeMessageModel(dto1, myUserProfile, emptyList())

        val dto2 = mockk<BisqEasyOpenTradeMessageDto>()
        every { dto2.chatMessageType } returns ChatMessageTypeEnum.TEXT
        every { dto2.senderUserProfile } returns createMockUserProfile("User2")
        every { dto2.messageId } returns "msg2"
        every { dto2.text } returns null
        every { dto2.citation } returns null
        every { dto2.date } returns 0L
        every { dto2.tradeId } returns "trade1"
        every { dto2.mediator } returns null
        every { dto2.bisqEasyOffer } returns null
        every { dto2.citationAuthorUserProfile } returns null
        val model2 = BisqEasyOpenTradeMessageModel(dto2, myUserProfile, emptyList())

        val dto3 = mockk<BisqEasyOpenTradeMessageDto>()
        every { dto3.chatMessageType } returns ChatMessageTypeEnum.TEXT
        every { dto3.senderUserProfile } returns createMockUserProfile("ignoredUser1")
        every { dto3.messageId } returns "msg3"
        every { dto3.text } returns null
        every { dto3.citation } returns null
        every { dto3.date } returns 0L
        every { dto3.tradeId } returns "trade1"
        every { dto3.mediator } returns null
        every { dto3.bisqEasyOffer } returns null
        every { dto3.citationAuthorUserProfile } returns null
        val model3 = BisqEasyOpenTradeMessageModel(dto3, myUserProfile, emptyList())

        val trade1MessagesFlow: StateFlow<Set<BisqEasyOpenTradeMessageModel>> = MutableStateFlow(setOf(model1, model2, model3))

        // Mock DTO and model for trade2
        val dto4 = mockk<BisqEasyOpenTradeMessageDto>()
        every { dto4.chatMessageType } returns ChatMessageTypeEnum.TEXT
        every { dto4.senderUserProfile } returns userProfileDemoObj.copy(userName = "User3", nym = "User3")
        every { dto4.messageId } returns "msg4"
        every { dto4.text } returns null
        every { dto4.citation } returns null
        every { dto4.date } returns 0L
        every { dto4.tradeId } returns "trade2"
        every { dto4.mediator } returns null
        every { dto4.bisqEasyOffer } returns null
        every { dto4.citationAuthorUserProfile } returns null
        val model4 = BisqEasyOpenTradeMessageModel(dto4, myUserProfile, emptyList())

        val trade2MessagesFlow = MutableStateFlow(setOf(model4))

        val channelModel1 = mockk<BisqEasyOpenTradeChannelModel>()
        every { channelModel1.chatMessages } answers { trade1MessagesFlow }

        val channelModel2 = mockk<BisqEasyOpenTradeChannelModel>()
        every { channelModel2.chatMessages } answers { trade2MessagesFlow }

        val trade1 = mockk<TradeItemPresentationModel>()
        every { trade1.tradeId } returns "trade1"
        every { trade1.bisqEasyOpenTradeChannelModel } returns channelModel1

        val trade2 = mockk<TradeItemPresentationModel>()
        every { trade2.tradeId } returns "trade2"
        every { trade2.bisqEasyOpenTradeChannelModel } returns channelModel2

        val openTradeItemsFlow = MutableStateFlow(listOf(trade1, trade2))
        every { tradesServiceFacade.openTradeItems } returns openTradeItemsFlow

        // Create presenter
        val presenter = MainPresenter(
            openTradesNotificationService,
            settingsService,
            tradesServiceFacade,
            userProfileServiceFacade,
            tradeReadStateRepository,
            urlLauncher
        )

        // Collect the unread messages map
        val unreadMap = presenter.tradesWithUnreadMessages.first()

        // Assertions
        // Trade1: 3 messages - 1 ignored = 2 visible, read 1, unread 1
        // Trade2: 1 message, read 0, unread 1
        assertEquals(mapOf("trade1" to 1, "trade2" to 1), unreadMap)
    }
}

