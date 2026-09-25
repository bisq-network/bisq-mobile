package network.bisq.mobile.presentation.report_user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportUserPresenterTest : PresentationKoinTestBase() {
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private lateinit var presenter: ReportUserPresenter
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private val reportedUser = createMockUserProfile("reportedUser")

    override fun onKoinReady() {
        presenter =
            ReportUserPresenter(
                mainPresenter = mainPresenter,
                userProfileServiceFacade = userProfileServiceFacade,
            )
        presenter.onViewAttached()
        presenter.initialize(reportedUser)
        presenter.onAction(ReportUserUiAction.OnMessageChange("This user violated chat rules"))
    }

    override fun onTearDown() {
        try {
            presenter.onViewUnattaching()
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

            presenter.onAction(ReportUserUiAction.OnReportClick)
            presenter.onAction(ReportUserUiAction.OnReportClick)
            runCurrent()

            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, any()) }
            assertFalse(presenter.isReportActionEnabled.value)
            assertTrue(presenter.uiState.value.isLoading)

            blocker.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `report failure shows an error snackbar and re-enables the report button for retry`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.failure(RuntimeException("network error"))

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            assertTrue(presenter.isReportActionEnabled.value)
            assertTrue(presenter.uiState.value.isReportMessageValid)
            assertFalse(presenter.uiState.value.isLoading)
            verify {
                globalUiManager.showSnackbar(
                    "mobile.chat.reportToModerator.error".i18n(),
                    SnackbarType.ERROR,
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `report success shows a confirmation snackbar and re-enables the report button`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.success(Unit)

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            assertTrue(presenter.isReportActionEnabled.value)
            assertFalse(presenter.uiState.value.isLoading)
            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, any()) }
            // The dialog closes on success, so the snackbar is the only trace the report left behind.
            verify {
                globalUiManager.showSnackbar(
                    "mobile.chat.reportToModerator.success".i18n(),
                    SnackbarType.SUCCESS,
                    any(),
                    any(),
                )
            }
        }

    /**
     * `UserProfileServiceFacade.reportUserProfile` documents a trimmed message. Trimming happens at
     * the call and nowhere else: the state keeps what the user typed, so a report that fails reopens
     * the dialog on their own text rather than on a silently edited copy.
     */
    @Test
    fun `the service receives a trimmed message while the typed draft is left alone`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.failure(RuntimeException("network error"))
            presenter.onAction(ReportUserUiAction.OnMessageChange(PADDED_MESSAGE))

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, TRIMMED_MESSAGE) }
            assertEquals(PADDED_MESSAGE, presenter.uiState.value.message)
        }

    @Test
    fun `a report opened from a message sends the trimmed reason followed by the message metadata`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns Result.success(Unit)
            presenter.initialize(reportedUser, reportedMessage = REPORTED_MESSAGE)
            presenter.onAction(ReportUserUiAction.OnMessageChange(PADDED_MESSAGE))

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                userProfileServiceFacade.reportUserProfile(reportedUser, REPORTED_MESSAGE.appendTo(TRIMMED_MESSAGE))
            }
        }

    @Test
    fun `a failed report from a message hands back only what the user typed`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns
                Result.failure(RuntimeException("network error"))
            presenter.initialize(reportedUser, reportedMessage = REPORTED_MESSAGE)
            presenter.onAction(ReportUserUiAction.OnMessageChange(PADDED_MESSAGE))
            val effect = async { presenter.effect.first() }
            runCurrent()

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            assertEquals(ReportUserEffect.ReportError(PADDED_MESSAGE), effect.await())
        }

    @Test
    fun `re-initializing without a message drops the earlier message metadata`() =
        runTest {
            coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns Result.success(Unit)
            presenter.initialize(reportedUser, reportedMessage = REPORTED_MESSAGE)
            presenter.initialize(reportedUser)
            presenter.onAction(ReportUserUiAction.OnMessageChange(TRIMMED_MESSAGE))

            presenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.reportUserProfile(reportedUser, TRIMMED_MESSAGE) }
        }

    @Test
    fun `report click before initialize completes without calling service`() =
        runTest {
            val uninitializedPresenter =
                ReportUserPresenter(
                    mainPresenter = mainPresenter,
                    userProfileServiceFacade = userProfileServiceFacade,
                )
            uninitializedPresenter.onAction(ReportUserUiAction.OnMessageChange("report text"))

            uninitializedPresenter.onAction(ReportUserUiAction.OnReportClick)
            advanceUntilIdle()

            coVerify(exactly = 0) { userProfileServiceFacade.reportUserProfile(any(), any()) }
            assertTrue(uninitializedPresenter.isReportActionEnabled.value)
        }

    private companion object {
        const val PADDED_MESSAGE = "  This user violated chat rules  "
        const val TRIMMED_MESSAGE = "This user violated chat rules"
        val REPORTED_MESSAGE =
            ReportedMessage(channel = "discussion.bisq", date = 1_234_567_890_123L, text = "buy my coin")
    }
}
