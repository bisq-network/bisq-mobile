package network.bisq.mobile.presentation.settings.ignored_users

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IgnoredUsersPresenterTest : PlatformPresentationKoinTestBase() {
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var presenter: IgnoredUsersPresenter

    override fun onKoinReady() {
        userProfileServiceFacade = mockk(relaxed = true)
        val mainPresenter: MainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        presenter = IgnoredUsersPresenter(userProfileServiceFacade, mainPresenter)
    }

    @Test
    fun `while ignored users are loading isLoading stays true and clears once loaded`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } coAnswers {
                delay(1000)
                setOf(user.networkId.pubKey.id)
            }
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)

            presenter.onViewAttached()
            assertTrue(presenter.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoading)
            assertEquals(listOf(user), presenter.uiState.value.ignoredUsers)
        }

    @Test
    fun `when loading ignored users fails then load failed is exposed and loading clears`() =
        runTest {
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } throws RuntimeException("no connection")

            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoading)
            assertTrue(presenter.uiState.value.isLoadFailed)
            assertTrue(
                presenter.uiState.value.ignoredUsers
                    .isEmpty(),
            )
        }

    @Test
    fun `retry after a failed load clears the error and shows the ignored users`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } throws RuntimeException("no connection")
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)

            presenter.onViewAttached()
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isLoadFailed)

            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            presenter.onAction(IgnoredUsersUiAction.OnRetryLoadClick)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoadFailed)
            assertFalse(presenter.uiState.value.isLoading)
            assertEquals(listOf(user), presenter.uiState.value.ignoredUsers)
        }

    @Test
    fun `unblock click opens the confirmation for that peer and dismiss closes it`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnUnblockClick(user.networkId.pubKey.id))
            assertEquals(user.networkId.pubKey.id, presenter.uiState.value.unblockUserId)

            presenter.onAction(IgnoredUsersUiAction.OnDismissUnblockDialog)
            assertNull(presenter.uiState.value.unblockUserId)
        }

    @Test
    fun `confirm without an open dialog does nothing`() =
        runTest {
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            advanceUntilIdle()

            coVerify(exactly = 0) { userProfileServiceFacade.undoIgnoreUserProfile(any()) }
        }

    @Test
    fun `peer profile click navigates to that peer`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnPeerProfileClick(user.networkId.pubKey.id))
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.PeerProfile(user.networkId.pubKey.id), any(), any()) }
        }

    @Test
    fun `a successful unblock closes the dialog and drops the peer without reloading`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            val other = createMockUserProfile("other-blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns
                setOf(user.networkId.pubKey.id, other.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user, other)

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnUnblockClick(user.networkId.pubKey.id))
            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(user.networkId.pubKey.id) }
            assertNull(presenter.uiState.value.unblockUserId)
            assertEquals(listOf(other), presenter.uiState.value.ignoredUsers)
            // The remaining peers stay on screen: no second fetch, so no spinner over the list.
            coVerify(exactly = 1) { userProfileServiceFacade.findUserProfiles(any()) }
            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `a failed unblock closes the dialog, keeps the peer and reports the error`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile(any()) } throws RuntimeException("fail")

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnUnblockClick(user.networkId.pubKey.id))
            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            advanceUntilIdle()

            assertNull(presenter.uiState.value.unblockUserId)
            assertEquals(listOf(user), presenter.uiState.value.ignoredUsers)
            assertTrue(presenter.uiState.value.isUnblockConfirmEnabled)
            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `the icon provider delegates to the facade`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            val icon: PlatformImage = mockk()
            coEvery { userProfileServiceFacade.getUserProfileIcon(user) } returns icon

            assertSame(icon, presenter.userProfileIconProvider(user))
        }

    @Test
    fun `rapid double-tap on unblock confirm calls undoIgnoreUserProfile only once`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile(user.networkId.pubKey.id) } coAnswers {
                delay(Long.MAX_VALUE)
            }

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnUnblockClick(user.networkId.pubKey.id))
            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(user.networkId.pubKey.id) }
            assertFalse(presenter.uiState.value.isUnblockConfirmEnabled)
        }

    @Test
    fun `unblock confirm failure re-enables confirm action`() =
        runTest {
            val user = createMockUserProfile("blocked-user")
            coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(user.networkId.pubKey.id)
            coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(user)
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile(user.networkId.pubKey.id) } throws RuntimeException("fail")

            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(IgnoredUsersUiAction.OnUnblockClick(user.networkId.pubKey.id))
            presenter.onAction(IgnoredUsersUiAction.OnConfirmUnblock)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isUnblockConfirmEnabled)
        }
}
