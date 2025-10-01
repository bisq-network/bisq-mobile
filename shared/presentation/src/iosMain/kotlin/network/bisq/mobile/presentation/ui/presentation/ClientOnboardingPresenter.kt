package network.bisq.mobile.presentation.ui.presentation

import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnboardingPresenter

class ClientOnboardingPresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService),
    IOnboardingPresenter {

    override val indexesToShow = listOf(0, 2)

    override val headline: String = "mobile.onboarding.clientMode.headline".i18n()

    override fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean) {
        if (isBisqUrlSet) {
            navigateToCreateProfile()
        } else {
            navigateTo(NavRoute.TrustedNodeSetup)
        }
    }

    override fun evaluateButtonText(deviceSettings: Settings?): String {
        return if (deviceSettings?.bisqApiUrl?.isNotEmpty() == true)
            "mobile.onboarding.createProfile".i18n()
        else
            "mobile.onboarding.setupConnection".i18n()
    }
}
