package network.bisq.mobile.presentation.settings.ignored_users

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IgnoredUsersPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var presenter: IgnoredUsersPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        userProfileServiceFacade = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }

        val mainPresenter: MainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        presenter = IgnoredUsersPresenter(userProfileServiceFacade, mainPresenter)
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }

    @Test
    fun `rapid double-tap on unblock confirm calls undoIgnoreUserProfile only once`() =
        runTest(testDispatcher) {
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
        runTest(testDispatcher) {
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
