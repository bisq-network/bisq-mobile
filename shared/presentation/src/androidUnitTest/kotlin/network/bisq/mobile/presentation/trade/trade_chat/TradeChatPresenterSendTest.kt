package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.NotificationController
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterSendTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade
    private lateinit var presenter: TradeChatPresenter
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tradeChatMessagesServiceFacade = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { globalUiManager }
                },
            )
        }

        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.data } returns MutableStateFlow(Settings())

        presenter =
            TradeChatPresenter(
                mainPresenter = mainPresenter,
                tradesServiceFacade = mockk(relaxed = true),
                tradeChatMessagesServiceFacade = tradeChatMessagesServiceFacade,
                settingsRepository = settingsRepository,
                tradeReadStateRepository = mockk(relaxed = true),
                userProfileServiceFacade = mockk(relaxed = true),
                notificationController = mockk<NotificationController>(relaxed = true),
                messageDeliveryServiceFacade = mockk<MessageDeliveryServiceFacade>(relaxed = true),
            )
        presenter.onViewAttached()
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `rapid double-tap on sendChatMessage triggers send only once`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.sendChatMessage("hello")
            advanceUntilIdle()

            assertTrue(presenter.isSendChatMessageEnabled.value)
        }
}
