package network.bisq.mobile.presentation.startup.onboarding

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_connect
import bisqapps.shared.presentation.generated.resources.img_p2p_tor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.pager_view.PagerViewItem
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

abstract class OnboardingPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val pages =
        listOf(
            PagerViewItem(
                title = "mobile.onboarding.teaserHeadline1".i18n(),
                image = Res.drawable.img_bisq_Easy,
                desc = "mobile.onboarding.line1".i18n(),
            ),
            // Shown at full mode
            PagerViewItem(
                title = "mobile.onboarding.fullMode.teaserHeadline".i18n(),
                image = Res.drawable.img_p2p_tor,
                desc = "mobile.onboarding.fullMode.line".i18n(),
            ),
            // Shown at client mode
            PagerViewItem(
                title = "mobile.onboarding.clientMode.teaserHeadline".i18n(),
                image = Res.drawable.img_connect,
                desc = "mobile.onboarding.clientMode.line".i18n(),
            ),
        )

    abstract val headline: String

    protected abstract val indexesToShow: List<Int>

    fun onAction(action: OnboardingUiAction) {
        when (action) {
            is OnboardingUiAction.OnPageChanged -> onPageChanged(action.page)
            is OnboardingUiAction.OnNextButtonClick -> onNextButtonClick()
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()

        val filteredPagesValue =
            pages.filterIndexed { index, _ ->
                indexesToShow.contains(index)
            }

        _uiState.update {
            it.copy(
                isLoading = false,
                filteredPages = filteredPagesValue,
                headline = headline,
                nextButtonText = "action.next".i18n(),
            )
        }
    }

    private fun onPageChanged(page: Int) {
        val state = _uiState.value
        val isLastPage = page == state.filteredPages.lastIndex
        val buttonText =
            if (isLastPage) {
                "mobile.onboarding.createProfile".i18n()
            } else {
                "action.next".i18n()
            }

        _uiState.update {
            it.copy(
                currentPage = page,
                nextButtonText = buttonText,
            )
        }
    }

    private fun onNextButtonClick() {
        // Page navigation happens in the UI
        // This method is called by the UI only on the final page to complete onboarding
        presenterScope.launch {
            showLoading()
            try {
                settingsRepository.setFirstLaunch(false)
                val hasProfile: Boolean = userProfileService.hasUserProfile()
                if (!hasProfile) {
                    navigateToCreateProfile()
                } else {
                    navigateToHome()
                }
            } finally {
                hideLoading()
            }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(NavRoute.CreateProfile(true))
    }

    protected fun navigateToHome() {
        navigateTo(NavRoute.TabContainer) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }
}
