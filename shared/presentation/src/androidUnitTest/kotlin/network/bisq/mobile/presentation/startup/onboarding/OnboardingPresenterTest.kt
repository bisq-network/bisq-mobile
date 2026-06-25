package network.bisq.mobile.presentation.startup.onboarding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
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
class OnboardingPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: TestOnboardingPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { mockk(relaxed = true) }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        settingsRepository = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        presenter =
            TestOnboardingPresenter(
                mainPresenter,
                settingsRepository,
                userProfileService,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `rapid double-tap on next button triggers setFirstLaunch only once`() =
        runTest(testDispatcher) {
            coEvery { settingsRepository.setFirstLaunch(false) } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
            }
            coEvery { userProfileService.hasUserProfile() } returns false

            presenter.onAction(OnboardingUiAction.OnNextButtonClick)
            presenter.onAction(OnboardingUiAction.OnNextButtonClick)
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsRepository.setFirstLaunch(false) }
            assertFalse(presenter.isNextButtonEnabled.value)
        }

    @Test
    fun `next button navigates home when profile exists`() =
        runTest(testDispatcher) {
            coEvery { settingsRepository.setFirstLaunch(false) } returns Unit
            coEvery { userProfileService.hasUserProfile() } returns true

            presenter.onAction(OnboardingUiAction.OnNextButtonClick)
            advanceUntilIdle()

            assertFalse(presenter.isNextButtonEnabled.value)
        }

    private class TestOnboardingPresenter(
        mainPresenter: MainPresenter,
        settingsRepository: SettingsRepository,
        userProfileService: UserProfileServiceFacade,
    ) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService) {
        override val headline: String = "test"
        override val indexesToShow: List<Int> = listOf(0)
    }
}
