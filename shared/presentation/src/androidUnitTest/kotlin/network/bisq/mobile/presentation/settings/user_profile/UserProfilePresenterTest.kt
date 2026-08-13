package network.bisq.mobile.presentation.settings.user_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfilePresenter.
 *
 * Uses [runCurrent] instead of [kotlinx.coroutines.test.advanceUntilIdle] after attach because
 * [UserProfilePresenter] starts [network.bisq.mobile.domain.utils.TimeUtils.tickerFlow], which
 * keeps scheduling delayed work and would hang [kotlinx.coroutines.test.advanceUntilIdle].
 *
 * A more elegant approach is to refactor the presenter to inject a testable ticker/clock so tests
 * can keep using [kotlinx.coroutines.test.advanceUntilIdle]. Not needed for this suite now —
 * consider it if more presenters follow the same ticker pattern.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePresenterTest : PresentationKoinTestBase() {
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val reputationServiceFacade: ReputationServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: UserProfilePresenter

    // Test data
    private val profile1 = createMockUserProfile("Alice")
    private val profile2 = createMockUserProfile("Bob")
    private val profile3 = createMockUserProfile("Charlie")

    override fun onKoinReady() {
        // Default mock behaviors
        every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { userProfileServiceFacade.numUserProfiles } returns MutableStateFlow(0)
        coEvery { userProfileServiceFacade.getUserProfileIcon(any(), any()) } returns mockk<PlatformImage>(relaxed = true)
        coEvery { userProfileServiceFacade.getUserPublishDate() } returns 0L
        coEvery { userProfileServiceFacade.selectUserProfile(any()) } returns Result.success(profile1)
        coEvery { userProfileServiceFacade.deleteUserProfile(any()) } returns Result.success(profile1)
        coEvery {
            userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
        } returns Result.success(profile1)
        coEvery { reputationServiceFacade.getReputation(any()) } returns
            Result.success(
                ReputationScoreVO(totalScore = 100L, fiveSystemScore = 50.0, ranking = 10),
            )
        coEvery { reputationServiceFacade.getProfileAge(any()) } returns Result.success(30L)
    }

    private fun createPresenter(): UserProfilePresenter =
        UserProfilePresenter(
            userProfileServiceFacade,
            reputationServiceFacade,
            mainPresenter,
        )

    /**
     * Runs [block] and always detaches the presenter, cancelling its jobs. The detach has to happen
     * inside [runTest]: the ticker keeps the scheduler busy, so a failed assertion would otherwise
     * hang runTest's cleanup instead of reporting the failure.
     */
    private fun runPresenterTest(block: suspend TestScope.() -> Unit) =
        runTest {
            try {
                block()
            } finally {
                if (::presenter.isInitialized) {
                    presenter.onViewUnattaching()
                    runCurrent()
                }
            }
        }

    // ========== UI State Initialization Tests ==========

    @Test
    fun `initial state is empty when no profiles exist`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.userProfiles.isEmpty())
            assertNull(state.selectedUserProfile)
            // Presenter sets isLoadingData=true on attach and only clears it when a non-null
            // profile is collected, so empty selection stays loading/busy. But no loading
            // indicator/content is shown since all controls are gated behind `uiState.selectedUserProfile?.let {}`
            assertTrue(state.isLoadingData)
            assertTrue(state.isBusy)
            assertFalse(state.shouldBlurBg)
        }

    @Test
    fun `initial state shows profiles when they exist`() =
        runPresenterTest {
            // Given
            val profiles = listOf(profile1, profile2, profile3)
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(profiles)
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // Then
            val state = presenter.uiState.value
            assertEquals(3, state.userProfiles.size)
            assertEquals(profile1, state.selectedUserProfile)
        }

    // ========== Profile Selection Tests ==========

    @Test
    fun `selecting a profile calls service and updates state on success`() =
        runPresenterTest {
            // Given
            val profiles = listOf(profile1, profile2)
            val selectedFlow = MutableStateFlow<UserProfileVO?>(profile1)
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(profiles)
            every { userProfileServiceFacade.selectedUserProfile } returns selectedFlow
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.networkId.pubKey.id) } coAnswers {
                selectedFlow.value = profile2
                Result.success(profile2)
            }

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            runCurrent()

            // Then
            coVerify { userProfileServiceFacade.selectUserProfile(profile2.networkId.pubKey.id) }
            assertEquals(profile2, presenter.uiState.value.selectedUserProfile)
        }

    @Test
    fun `selecting a profile handles failure gracefully`() =
        runPresenterTest {
            // Given
            val profiles = listOf(profile1, profile2)
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(profiles)
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.networkId.pubKey.id) } returns
                Result.failure(Exception("Network error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            runCurrent()

            // Then - should still have profile1 selected
            assertEquals(profile1, presenter.uiState.value.selectedUserProfile)
        }

    // ========== Profile Update Tests ==========

    @Test
    fun `updating profile statement and terms calls service with correct profileId`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(
                    profile1.networkId.pubKey.id,
                    "New statement",
                    "New terms",
                )
            } returns Result.success(profile1.copy(statement = "New statement", terms = "New terms"))

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When - update drafts
            presenter.onAction(UserProfileUiAction.OnStatementChange("New statement"))
            presenter.onAction(UserProfileUiAction.OnTermsChange("New terms"))
            runCurrent()

            // Then - verify drafts updated
            assertEquals("New statement", presenter.uiState.value.statementDraft)
            assertEquals("New terms", presenter.uiState.value.termsDraft)

            // When - save
            presenter.onAction(
                UserProfileUiAction.OnSavePress,
            )
            runCurrent()

            // Then
            coVerify {
                userProfileServiceFacade.updateAndPublishUserProfile(
                    profile1.networkId.pubKey.id,
                    "New statement",
                    "New terms",
                )
            }
            assertFalse(presenter.uiState.value.isBusy)
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `updating profile sets isBusy during operation`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
            } returns Result.success(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onAction(
                UserProfileUiAction.OnSavePress,
            )

            // Then - guarded in-flight work disables actions (uiState.isBusy is only isLoadingData)
            assertFalse(presenter.isActionEnabled.value)

            runCurrent()

            // Then - should re-enable after completion
            assertTrue(presenter.isActionEnabled.value)
            assertFalse(presenter.uiState.value.isBusy)
        }

    // ========== Profile Deletion Tests ==========

    @Test
    fun `delete action shows confirmation dialog`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnDeletePress)
            runCurrent()

            presenter.onAction(UserProfileUiAction.OnUserProfileSelect(profile2))
            runCurrent()

            // Then
            assertEquals(profile1, presenter.uiState.value.showDeleteConfirmationForProfile)
        }

    @Test
    fun `delete confirmation dismissal clears dialog`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            presenter.onAction(UserProfileUiAction.OnDeletePress)
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirmationDismiss)
            runCurrent()

            // Then
            assertNull(presenter.uiState.value.showDeleteConfirmationForProfile)
        }

    @Test
    fun `delete confirmed calls service and updates state on success`() =
        runPresenterTest {
            // Given
            val profilesFlow = MutableStateFlow(listOf(profile1, profile2, profile3))
            every { userProfileServiceFacade.userProfiles } returns profilesFlow
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile2)
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) } coAnswers {
                profilesFlow.value = listOf(profile1, profile3) // Simulate removal
                Result.success(profile1) // Returns newly selected profile
            }

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            presenter.onAction(UserProfileUiAction.OnDeletePress)
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirm)
            runCurrent()

            // Then
            coVerify { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) }
            assertNull(presenter.uiState.value.showDeleteConfirmationForProfile)
            assertFalse(presenter.uiState.value.isBusy)
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `delete confirmed shows error dialog on failure`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile2)
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) } returns
                Result.failure(Exception("Cannot delete last profile"))

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            presenter.onAction(UserProfileUiAction.OnDeletePress)
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirm)
            runCurrent()

            // Then
            assertTrue(presenter.uiState.value.showDeleteErrorDialog)
            assertFalse(presenter.uiState.value.isBusy)
            assertTrue(presenter.isActionEnabled.value)
        }

    @Test
    fun `delete error dialog can be dismissed`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            presenter.onAction(UserProfileUiAction.OnDeleteError)
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismiss)
            runCurrent()

            // Then
            assertFalse(presenter.uiState.value.showDeleteErrorDialog)
        }

    // ========== Create Profile Navigation Tests ==========

    @Test
    fun `create profile action navigates to CreateProfile screen`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onAction(UserProfileUiAction.OnCreateProfilePress)
            runCurrent()

            // Then
            verify { navigationManager.navigate(NavRoute.CreateProfile(false), any(), any()) }
        }

    // ========== Lifecycle Tests ==========

    @Test
    fun `jobs are cancelled on view detach`() =
        runPresenterTest {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            runCurrent()

            // When
            presenter.onViewUnattaching()
            // Disposal is launched on Main — run it so cancellation actually executes.
            runCurrent()

            // Then - no exceptions should be thrown; jobs cancel gracefully
        }
}
