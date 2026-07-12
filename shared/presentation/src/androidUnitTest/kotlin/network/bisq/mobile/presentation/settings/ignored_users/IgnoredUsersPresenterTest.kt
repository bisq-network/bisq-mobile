package network.bisq.mobile.presentation.settings.ignored_users

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.coroutines.PlatformPresentationKoinTestBase
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertFalse
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

            presenter.unblockUserConfirm(user.networkId.pubKey.id)
            presenter.unblockUserConfirm(user.networkId.pubKey.id)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(user.networkId.pubKey.id) }
            assertFalse(presenter.isUnblockUserConfirmEnabled.value)
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

            presenter.unblockUserConfirm(user.networkId.pubKey.id)
            advanceUntilIdle()

            assertTrue(presenter.isUnblockUserConfirmEnabled.value)
        }
}
