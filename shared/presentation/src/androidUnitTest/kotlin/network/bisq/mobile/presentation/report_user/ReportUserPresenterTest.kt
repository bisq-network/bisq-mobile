package network.bisq.mobile.presentation.report_user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
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
class ReportUserPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var presenter: ReportUserPresenter
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val globalUiManager by lazy { GlobalUiManager(testDispatcher) }

    private val reportedUser = createMockUserProfile("reportedUser")
    private val chatMessage =
        BisqEasyOpenTradeMessageModel(
            mockk<BisqEasyOpenTradeMessageDto> {
                every { chatMessageType } returns ChatMessageTypeEnum.TEXT
                every { senderUserProfile } returns reportedUser
                every { messageId } returns "msg1"
                every { text } returns "bad message"
                every { citation } returns null
                every { date } returns 1000L
                every { tradeId } returns "trade1"
                every { mediator } returns null
                every { bisqEasyOffer } returns null
                every { citationAuthorUserProfile } returns null
            },
            createMockUserProfile("myUser"),
            emptyList(),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userProfileServiceFacade = mockk(relaxed = true)

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

        I18nSupport.initialize("en")

        presenter =
            ReportUserPresenter(
                mainPresenter = mainPresenter,
                userProfileServiceFacade = userProfileServiceFacade,
            )
        presenter.onViewAttached()
        presenter.initialize(chatMessage, null)
        presenter.onMessageChange("This user violated chat rules")
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
    fun `rapid double-tap on onReportClick triggers reportUserProfile only once`() =
        runTest(testDispatcher) {
            val blocker = CompletableDeferred<Unit>()
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } coAnswers {
                blocker.await()
                Result.success(Unit)
            }

            presenter.onReportClick()
            presenter.onReportClick()
            runCurrent()

            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, any()) }
            assertFalse(presenter.isReportActionEnabled.value)
            assertTrue(presenter.uiState.value.isLoading)

            blocker.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `report failure re-enables report button for retry`() =
        runTest(testDispatcher) {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.onReportClick()
            advanceUntilIdle()

            assertTrue(presenter.isReportActionEnabled.value)
            assertTrue(presenter.uiState.value.isReportMessageValid)
            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `report success completes and re-enables report button`() =
        runTest(testDispatcher) {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.success(Unit)

            presenter.onReportClick()
            advanceUntilIdle()

            assertTrue(presenter.isReportActionEnabled.value)
            assertFalse(presenter.uiState.value.isLoading)
            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, any()) }
        }

    @Test
    fun `report click before initialize completes without calling service`() =
        runTest(testDispatcher) {
            val uninitializedPresenter =
                ReportUserPresenter(
                    mainPresenter = mainPresenter,
                    userProfileServiceFacade = userProfileServiceFacade,
                )
            uninitializedPresenter.onMessageChange("report text")

            uninitializedPresenter.onReportClick()
            advanceUntilIdle()

            coVerify(exactly = 0) { userProfileServiceFacade.reportUserProfile(any(), any()) }
            assertTrue(uninitializedPresenter.isReportActionEnabled.value)
        }
}
