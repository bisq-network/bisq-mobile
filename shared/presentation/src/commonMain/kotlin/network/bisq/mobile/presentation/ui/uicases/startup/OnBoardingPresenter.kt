package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.navigation.Routes

open class OnBoardingPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    companion object {
        val CREATE_PROFILE_TEXT: String
            get() = "mobile.onboarding.createProfile".i18n()
        val SETUP_CONNECTION_TEXT: String
            get() = "mobile.onboarding.setupConnection".i18n()
    }

    override val indexesToShow = listOf(0)

    private val _buttonText = MutableStateFlow(CREATE_PROFILE_TEXT)
    override val buttonText: StateFlow<String> = _buttonText

    override val onBoardingData = listOf(
        PagerViewItem(
            title = "onboarding.bisq2.teaserHeadline1".i18n(),
            image = Res.drawable.img_bisq_Easy,
            desc = "mobile.onboarding.bisq2.line1".i18n()
        ),
        PagerViewItem(
            title = "mobile.onboarding.bisq2.bisqP2PInMobile".i18n(),
            image = Res.drawable.img_learn_and_discover,
            desc = "mobile.onboarding.line2".i18n()
        ),
        PagerViewItem(
            title = "onboarding.bisq2.teaserHeadline3".i18n(),
            image = Res.drawable.img_fiat_btc,
            desc = "onboarding.bisq2.line3".i18n()
        )
    )

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            settingsRepository.fetch()
            val deviceSettings: Settings? = settingsRepository.data.value
            _buttonText.value = mainButtonText(deviceSettings)
        }
    }

    protected open fun mainButtonText(deviceSettings: Settings?): String {
        return if (deviceSettings?.bisqApiUrl?.isNotEmpty() == true)
            CREATE_PROFILE_TEXT
        else
            SETUP_CONNECTION_TEXT
    }

    override fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState) {
        launchIO {
            settingsRepository.fetch()
            val deviceSettings: Settings? = settingsRepository.data.value

            val hasProfile: Boolean = userProfileService.hasUserProfile()

            if (pagerState.currentPage == indexesToShow.lastIndex) {

                // to ensure event propagation, probably need to change settings equals definition to avoid this
                val updatedSettings = Settings().apply {
                    bisqApiUrl = deviceSettings?.bisqApiUrl ?: ""
                    firstLaunch = false
                }

                settingsRepository.update(updatedSettings)

                val remoteBisqUrl = deviceSettings?.bisqApiUrl ?: ""
                doCustomNavigationLogic(remoteBisqUrl.isNotEmpty(), hasProfile)

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

    protected fun navigateToTrustedNodeSetup() {
        navigateTo(Routes.TrustedNodeSetup)
    }

    open fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean) {
        if (isBisqUrlSet) {
            navigateToCreateProfile()
        } else {
            navigateToTrustedNodeSetup()
        }
    }

}
