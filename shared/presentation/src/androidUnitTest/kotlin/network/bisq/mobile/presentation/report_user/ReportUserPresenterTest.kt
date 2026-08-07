package network.bisq.mobile.presentation.report_user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportUserPresenterTest : PresentationKoinTestBase() {
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private lateinit var presenter: ReportUserPresenter
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

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

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        presenter =
            ReportUserPresenter(
                mainPresenter = mainPresenter,
                userProfileServiceFacade = userProfileServiceFacade,
            )
        presenter.onViewAttached()
        presenter.initialize(chatMessage, null)
        presenter.onMessageChange("This user violated chat rules")
    }

    override fun onTearDown() {
        try {
            presenter.onViewUnattaching()
            globalUiManager.dispose()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `rapid double-tap on onReportClick triggers reportUserProfile only once`() =
        runTest {
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
        runTest {
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
        runTest {
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
        runTest {
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
