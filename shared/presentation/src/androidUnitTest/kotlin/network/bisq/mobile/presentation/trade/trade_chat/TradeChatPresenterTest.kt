package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterTest : PresentationKoinTestBase() {
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
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { settingsRepository.data } returns MutableStateFlow(mockk(relaxed = true))

        presenter =
            TradeChatPresenter(
                mainPresenter,
                tradesServiceFacade,
                tradeChatMessagesServiceFacade,
                settingsRepository,
                tradeReadStateRepository,
                userProfileServiceFacade,
                notificationController,
                messageDeliveryServiceFacade,
            )
    }

    @Test
    fun `rapid double-tap on sendChatMessage triggers send only once`() =
        runTest {
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.sendChatMessage("hello")
            presenter.sendChatMessage("hello")
            advanceUntilIdle()

            coVerify { tradeChatMessagesServiceFacade.sendChatMessage("hello", null) }
            assertFalse(presenter.isSendChatMessageEnabled.value)
        }

    @Test
    fun `sendChatMessage success clears quoted message`() =
        runTest {
            val quoted = mockk<network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel>(relaxed = true)
            every { quoted.text } returns "quoted"
            every { quoted.id } returns "q1"
            every { quoted.senderUserProfileId } returns "sender"
            presenter.onReply(quoted)
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } returns
                Result.success(Unit)

            presenter.sendChatMessage("hello")
            advanceUntilIdle()

            assertNull(presenter.quotedMessage.value)
        }

    @Test
    fun `confirmed ignore user calls ignoreUserProfile`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile("peer-1") } returns Unit

            presenter.showIgnoreUserPopup("peer-1")
            presenter.onConfirmedIgnoreUser("peer-1")
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.ignoreUserProfile("peer-1") }
        }

    @Test
    fun `confirmed undo ignore user calls undoIgnoreUserProfile`() =
        runTest {
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") } returns Unit

            presenter.showUndoIgnoreUserPopup("peer-2")
            presenter.onConfirmedUndoIgnoreUser("peer-2")
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") }
        }
}
