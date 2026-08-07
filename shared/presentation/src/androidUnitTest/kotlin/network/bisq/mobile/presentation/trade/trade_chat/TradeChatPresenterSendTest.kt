package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterSendTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade = mockk(relaxed = true)
    private lateinit var presenter: TradeChatPresenter

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        every { settingsRepository.data } returns MutableStateFlow(Settings())

        presenter =
            TradeChatPresenter(
                mainPresenter = mainPresenter,
                tradesServiceFacade = tradesServiceFacade,
                tradeChatMessagesServiceFacade = tradeChatMessagesServiceFacade,
                settingsRepository = settingsRepository,
                tradeReadStateRepository = tradeReadStateRepository,
                userProfileServiceFacade = userProfileServiceFacade,
                notificationController = notificationController,
                messageDeliveryServiceFacade = messageDeliveryServiceFacade,
            )
        presenter.onViewAttached()
    }

    override fun onTearDown() {
        try {
            presenter.onViewUnattaching()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `rapid double-tap on sendChatMessage triggers send only once`() =
        runTest {
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } coAnswers {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.sendChatMessage("hello")
            presenter.sendChatMessage("hello")
            advanceUntilIdle()

            coVerify(exactly = 1) { tradeChatMessagesServiceFacade.sendChatMessage("hello", null) }
            assertFalse(presenter.isSendChatMessageEnabled.value)
        }

    @Test
    fun `sendChatMessage failure re-enables send button for retry`() =
        runTest {
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.sendChatMessage("hello")
            advanceUntilIdle()

            assertTrue(presenter.isSendChatMessageEnabled.value)
        }
}
