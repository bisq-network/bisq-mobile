package network.bisq.mobile.client.presentation

import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter

class ClientOnBoardingPresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
) : OnBoardingPresenter(mainPresenter, settingsRepository, userProfileService), IOnboardingPresenter {

    override val indexesToShow = listOf(0, 2)

    override val headline: String = "mobile.onboarding.clientMode.headline".i18n()

    override fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean) {
        if (isBisqUrlSet) {
            navigateToCreateProfile()
        } else {
            navigateTo(Routes.TrustedNodeSetup)
        }
    }

    override fun evaluateButtonText(deviceSettings: Settings?): String {
        return if (deviceSettings?.bisqApiUrl?.isNotEmpty() == true)
            "mobile.onboarding.createProfile".i18n()
        else
            "mobile.onboarding.setupConnection".i18n()
    }
}
