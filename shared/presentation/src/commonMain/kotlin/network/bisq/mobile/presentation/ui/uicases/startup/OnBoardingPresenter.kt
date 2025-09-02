package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bisq_Easy
import bisqapps.shared.presentation.generated.resources.img_connect
import bisqapps.shared.presentation.generated.resources.img_fiat_btc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.navigation.Routes

abstract class OnBoardingPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    private val _buttonText = MutableStateFlow("")
    override val buttonText: StateFlow<String> get() = _buttonText.asStateFlow()

    private val pages = listOf(
        PagerViewItem(
            title = "mobile.onboarding.teaserHeadline1".i18n(),
            image = Res.drawable.img_bisq_Easy,
            desc = "mobile.onboarding.line1".i18n()
        ),
        // Shown at full mode
        PagerViewItem(
            title = "mobile.onboarding.fullMode.teaserHeadline".i18n(),
            image = Res.drawable.img_fiat_btc,
            desc = "mobile.onboarding.fullMode.line".i18n()
        ),
        // Shown at client mode
        PagerViewItem(
            title = "mobile.onboarding.clientMode.teaserHeadline".i18n(),
            image = Res.drawable.img_connect,
            desc = "mobile.onboarding.clientMode.line".i18n()
        )
    )

    override var filteredPages: List<PagerViewItem> = listOf()

    override fun onViewAttached() {
        super.onViewAttached()

        filteredPages = pages.filterIndexed { index, _ ->
            indexesToShow.contains(index)
        }

        launchIO {
            val deviceSettings = settingsRepository.fetch()
            _buttonText.value = evaluateButtonText(deviceSettings)
        }
    }

    override fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState) {
        launchIO {
            if (pagerState.currentPage == filteredPages.lastIndex) {
                val deviceSettings = settingsRepository.fetch()

                settingsRepository.setFirstLaunch(false)

                val remoteBisqUrl = deviceSettings.bisqApiUrl
                val hasProfile: Boolean = userProfileService.hasUserProfile()
                launchUI {
                    doCustomNavigationLogic(remoteBisqUrl.isNotEmpty(), hasProfile)
                }
            } else {
                // Let the UI handle the animation in the composable
                // This is safe because we're using the coroutineScope passed from the composable
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(Routes.CreateProfile)
    }

    abstract fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean)

    abstract fun evaluateButtonText(deviceSettings: Settings?): String
}
