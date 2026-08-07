package network.bisq.mobile.presentation.settings.user_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePresenterActionGuardTest : PresentationKoinTestBase() {
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val reputationServiceFacade: ReputationServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: UserProfilePresenter

    private val profile1 = createMockUserProfile("Alice")
    private val profile2 = createMockUserProfile("Bob")

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { userProfileServiceFacade.numUserProfiles } returns MutableStateFlow(0)
        coEvery { reputationServiceFacade.getReputation(any()) } returns
            Result.success(ReputationScoreVO(totalScore = 100L, fiveSystemScore = 50.0, ranking = 10))
        coEvery { reputationServiceFacade.getProfileAge(any()) } returns Result.success(30L)

        presenter =
            UserProfilePresenter(
                userProfileServiceFacade,
                reputationServiceFacade,
                mainPresenter,
            )
        setUiState(
            UserProfileUiState(
                selectedUserProfile = profile1,
                statementDraft = "Hello",
                termsDraft = "Terms",
            ),
        )
    }

    @Test
    fun `rapid double-tap on save triggers updateAndPublish only once`() =
        runTest {
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
            } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
                Result.success(profile1)
            }

            presenter.onAction(UserProfileUiAction.OnSavePress)
            presenter.onAction(UserProfileUiAction.OnSavePress)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                userProfileServiceFacade.updateAndPublishUserProfile(profile1.id, "Hello", "Terms")
            }
            assertFalse(presenter.isActionEnabled.value)
        }

    @Test
    fun `save failure re-enables action guard`() =
        runTest {
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
            } returns Result.failure(RuntimeException("save failed"))

            presenter.onAction(UserProfileUiAction.OnSavePress)
            advanceUntilIdle()

            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `delete confirm calls service and re-enables guard on failure`() =
        runTest {
            setUiState(
                UserProfileUiState(
                    selectedUserProfile = profile2,
                    showDeleteConfirmationForProfile = profile2,
                ),
            )
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.id) } returns
                Result.failure(RuntimeException("delete failed"))

            presenter.onAction(UserProfileUiAction.OnDeleteConfirm)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.showDeleteErrorDialog)
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `profile select failure re-enables action guard`() =
        runTest {
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.id) } returns
                Result.failure(RuntimeException("select failed"))

            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            advanceUntilIdle()

            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `profile select success completes guarded action`() =
        runTest {
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.id) } returns
                Result.success(profile2)

            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.selectUserProfile(profile2.id) }
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `delete confirm success shows success snackbar path`() =
        runTest {
            setUiState(
                UserProfileUiState(
                    selectedUserProfile = profile2,
                    showDeleteConfirmationForProfile = profile2,
                ),
            )
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.id) } returns
                Result.success(profile1)

            presenter.onAction(UserProfileUiAction.OnDeleteConfirm)
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.deleteUserProfile(profile2.id) }
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `delete confirm exception shows error dialog`() =
        runTest {
            setUiState(
                UserProfileUiState(
                    selectedUserProfile = profile2,
                    showDeleteConfirmationForProfile = profile2,
                ),
            )
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.id) } throws
                RuntimeException("delete exploded")

            presenter.onAction(UserProfileUiAction.OnDeleteConfirm)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.showDeleteErrorDialog)
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `profile select exception is logged and does not crash`() =
        runTest {
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.id) } throws
                RuntimeException("select exploded")

            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            advanceUntilIdle()

            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `save with N A values strips localized placeholders before publish`() =
        runTest {
            setUiState(
                UserProfileUiState(
                    selectedUserProfile = profile1,
                    statementDraft = UserProfilePresenter.getLocalizedNA(),
                    termsDraft = UserProfilePresenter.getLocalizedNA(),
                ),
            )
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
            } returns Result.success(profile1)

            presenter.onAction(UserProfileUiAction.OnSavePress)
            advanceUntilIdle()

            coVerify {
                userProfileServiceFacade.updateAndPublishUserProfile(profile1.id, "", "")
            }
        }

    private fun setUiState(state: UserProfileUiState) {
        val field = UserProfilePresenter::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(presenter) as MutableStateFlow<UserProfileUiState>).value = state
    }
}
