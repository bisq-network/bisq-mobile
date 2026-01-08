package network.bisq.mobile.presentation.settings.user_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfilePresenter.
 *
 * NOTE: These tests are currently ignored due to a technical limitation with the presenter's design.
 * The UserProfilePresenter.launchJobs() method starts several infinite coroutine collect loops:
 * 1. Collecting from userProfileServiceFacade.selectedUserProfile
 * 2. Collecting from userProfileServiceFacade.userProfiles
 * 3. Collecting from TimeUtils.tickerFlow (which emits every second indefinitely)
 *
 * These infinite loops cause tests to hang when onViewAttached() is called, even with
 * StandardTestDispatcher and advanceUntilIdle(). The tickerFlow in particular never completes.
 *
 * Solutions to consider:
 * 1. Refactor the presenter to inject a testable ticker/clock
 * 2. Make the presenter's coroutine scopes more testable
 * 3. Focus on integration tests (see MultiProfileIntegrationTest) instead of unit tests
 *
 * For now, use MultiProfileIntegrationTest which provides good coverage of the multi-profile
 * feature's core functionality without the presenter's infinite loop issues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: UserProfilePresenter

    // Test data
    private val profile1 = createMockUserProfile("Alice")
    private val profile2 = createMockUserProfile("Bob")
    private val profile3 = createMockUserProfile("Charlie")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                },
            )
        }

        // Setup mocks
        userProfileServiceFacade = mockk(relaxed = true)
        reputationServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        // Default mock behaviors
        every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
        every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { userProfileServiceFacade.numUserProfiles } returns MutableStateFlow(0)
        coEvery { userProfileServiceFacade.getUserProfileIcon(any(), any()) } returns mockk<PlatformImage>(relaxed = true)
        coEvery { userProfileServiceFacade.getUserPublishDate() } returns 0L
        coEvery { reputationServiceFacade.getReputation(any()) } returns
            Result.success(
                ReputationScoreVO(totalScore = 100L, fiveSystemScore = 50.0, ranking = 10),
            )
        coEvery { reputationServiceFacade.getProfileAge(any()) } returns Result.success(30L)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): UserProfilePresenter =
        UserProfilePresenter(
            userProfileServiceFacade,
            reputationServiceFacade,
            mainPresenter,
        )

    // ========== UI State Initialization Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `initial state is empty when no profiles exist`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(null)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.userProfiles.isEmpty())
            assertNull(state.selectedUserProfile)
            assertFalse(state.isBusy)
            assertFalse(state.shouldBlurBg)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `initial state shows profiles when they exist`() =
        runTest(testDispatcher) {
            // Given
            val profiles = listOf(profile1, profile2, profile3)
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(profiles)
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(3, state.userProfiles.size)
            assertEquals(profile1, state.selectedUserProfile)
        }

    // ========== Profile Selection Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `selecting a profile calls service and updates state on success`() =
        runTest(testDispatcher) {
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
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnUserProfileSelected(profile2))
            advanceUntilIdle()

            // Then
            coVerify { userProfileServiceFacade.selectUserProfile(profile2.networkId.pubKey.id) }
            assertEquals(profile2, presenter.uiState.value.selectedUserProfile)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `selecting a profile handles failure gracefully`() =
        runTest(testDispatcher) {
            // Given
            val profiles = listOf(profile1, profile2)
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(profiles)
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery { userProfileServiceFacade.selectUserProfile(profile2.networkId.pubKey.id) } returns
                Result.failure(Exception("Network error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnUserProfileSelected(profile2))
            advanceUntilIdle()

            // Then - should still have profile1 selected
            assertEquals(profile1, presenter.uiState.value.selectedUserProfile)
        }

    // ========== Profile Update Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `updating profile statement and terms calls service with correct profileId`() =
        runTest(testDispatcher) {
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
            advanceUntilIdle()

            // When - update drafts
            presenter.onAction(UserProfileUiAction.OnStatementChanged("New statement"))
            presenter.onAction(UserProfileUiAction.OnTermsChanged("New terms"))
            advanceUntilIdle()

            // Then - verify drafts updated
            assertEquals("New statement", presenter.uiState.value.statementDraft)
            assertEquals("New terms", presenter.uiState.value.termsDraft)

            // When - save
            presenter.onAction(
                UserProfileUiAction.OnSavePressed(
                    profileId = profile1.networkId.pubKey.id,
                    uiState = presenter.uiState.value,
                ),
            )
            advanceUntilIdle()

            // Then
            coVerify {
                userProfileServiceFacade.updateAndPublishUserProfile(
                    profile1.networkId.pubKey.id,
                    "New statement",
                    "New terms",
                )
            }
            assertFalse(presenter.uiState.value.isBusy)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `updating profile sets isBusy during operation`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery {
                userProfileServiceFacade.updateAndPublishUserProfile(any(), any(), any())
            } returns Result.success(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(
                UserProfileUiAction.OnSavePressed(
                    profileId = profile1.networkId.pubKey.id,
                    uiState = presenter.uiState.value.copy(statementDraft = "test"),
                ),
            )

            // Then - should be busy immediately
            assertTrue(presenter.uiState.value.isBusy)

            advanceUntilIdle()

            // Then - should not be busy after completion
            assertFalse(presenter.uiState.value.isBusy)
        }

    // ========== Profile Deletion Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `delete action shows confirmation dialog`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnDeletePressed(profile2))
            advanceUntilIdle()

            // Then
            assertEquals(profile2, presenter.uiState.value.showDeleteConfirmationForProfile)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `delete confirmation dismissal clears dialog`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(UserProfileUiAction.OnDeletePressed(profile2))
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirmationDismissed)
            advanceUntilIdle()

            // Then
            assertNull(presenter.uiState.value.showDeleteConfirmationForProfile)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `delete confirmed calls service and updates state on success`() =
        runTest(testDispatcher) {
            // Given
            val profilesFlow = MutableStateFlow(listOf(profile1, profile2, profile3))
            every { userProfileServiceFacade.userProfiles } returns profilesFlow
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) } coAnswers {
                profilesFlow.value = listOf(profile1, profile3) // Simulate removal
                Result.success(profile1) // Returns newly selected profile
            }

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirmed(profile2))
            advanceUntilIdle()

            // Then
            coVerify { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) }
            assertNull(presenter.uiState.value.showDeleteConfirmationForProfile)
            assertFalse(presenter.uiState.value.isBusy)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `delete confirmed shows error dialog on failure`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1, profile2))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)
            coEvery { userProfileServiceFacade.deleteUserProfile(profile2.networkId.pubKey.id) } returns
                Result.failure(Exception("Cannot delete last profile"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteConfirmed(profile2))
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.showDeleteErrorDialog)
            assertFalse(presenter.uiState.value.isBusy)
        }

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `delete error dialog can be dismissed`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(UserProfileUiAction.OnDeleteError)
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnDeleteErrorDialogDismissed)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.showDeleteErrorDialog)
        }

    // ========== Create Profile Navigation Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `create profile action navigates to CreateProfile screen`() =
        runTest(testDispatcher) {
            // Given
            val navManager = mockk<NavigationManager>(relaxed = true)
            stopKoin()
            startKoin {
                modules(
                    module {
                        single<NavigationManager> { navManager }
                        single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    },
                )
            }

            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(UserProfileUiAction.OnCreateProfilePressed)
            advanceUntilIdle()

            // Then
            // TODO: Add verification when test becomes runnable:
            // verify { navManager.navigate(CreateProfile) }
        }

    // ========== Lifecycle Tests ==========

    @Ignore("Presenter has infinite coroutine loops that cause tests to hang - see class documentation")
    @Test
    fun `jobs are cancelled on view detach`() =
        runTest(testDispatcher) {
            // Given
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(profile1))
            every { userProfileServiceFacade.selectedUserProfile } returns MutableStateFlow(profile1)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onViewUnattaching()
            advanceUntilIdle()

            // Then - no exceptions should be thrown
            // Jobs should be cancelled gracefully
        }
}
