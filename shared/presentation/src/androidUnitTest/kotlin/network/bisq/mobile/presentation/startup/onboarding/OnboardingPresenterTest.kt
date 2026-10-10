package network.bisq.mobile.presentation.startup.onboarding

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPresenterTest : PresentationKoinTestBase() {
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val userProfileService: UserProfileServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: TestOnboardingPresenter

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        presenter =
            TestOnboardingPresenter(
                mainPresenter,
                settingsRepository,
                userProfileService,
            )
    }

    @Test
    fun `rapid double-tap on next button triggers setFirstLaunch only once`() =
        runTest {
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
        runTest {
            coEvery { settingsRepository.setFirstLaunch(false) } returns Unit
            coEvery { userProfileService.hasUserProfile() } returns true

            presenter.onAction(OnboardingUiAction.OnNextButtonClick)
            advanceUntilIdle()

            assertFalse(presenter.isNextButtonEnabled.value)
        }

    @Test
    fun `last page button offers to get started when a profile already exists`() =
        runTest {
            coEvery { userProfileService.hasUserProfile() } returns true

            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onAction(OnboardingUiAction.OnPageChanged(presenter.uiState.value.filteredPages.lastIndex))

            assertEquals("mobile.onboarding.getStarted".i18n(), presenter.uiState.value.nextButtonText)
        }

    @Test
    fun `last page button offers to create a profile when none exists`() =
        runTest {
            coEvery { userProfileService.hasUserProfile() } returns false

            presenter.onViewAttached()
            advanceUntilIdle()
            presenter.onAction(OnboardingUiAction.OnPageChanged(presenter.uiState.value.filteredPages.lastIndex))

            assertEquals("mobile.onboarding.createProfile".i18n(), presenter.uiState.value.nextButtonText)
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
