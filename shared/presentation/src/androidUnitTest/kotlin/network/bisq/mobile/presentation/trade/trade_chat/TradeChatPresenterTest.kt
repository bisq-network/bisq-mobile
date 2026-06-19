package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: TradeChatPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }

        tradesServiceFacade = mockk(relaxed = true)
        tradeChatMessagesServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { mockk<SettingsRepository>(relaxed = true).data } returns MutableStateFlow(mockk(relaxed = true))

        presenter =
            TradeChatPresenter(
                mainPresenter,
                tradesServiceFacade,
                tradeChatMessagesServiceFacade,
                mockk<SettingsRepository>(relaxed = true) {
                    every { data } returns MutableStateFlow(mockk(relaxed = true))
                },
                mockk<TradeReadStateRepository>(relaxed = true),
                userProfileServiceFacade,
                mockk<NotificationController>(relaxed = true),
                mockk<MessageDeliveryServiceFacade>(relaxed = true),
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid double-tap on sendChatMessage triggers send only once`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            coEvery { userProfileServiceFacade.ignoreUserProfile("peer-1") } returns Unit

            presenter.showIgnoreUserPopup("peer-1")
            presenter.onConfirmedIgnoreUser("peer-1")
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.ignoreUserProfile("peer-1") }
        }

    @Test
    fun `confirmed undo ignore user calls undoIgnoreUserProfile`() =
        runTest(testDispatcher) {
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") } returns Unit

            presenter.showUndoIgnoreUserPopup("peer-2")
            presenter.onConfirmedUndoIgnoreUser("peer-2")
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") }
        }
}
