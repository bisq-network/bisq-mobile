package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnboardingPresenter

class NodeOnboardingPresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService),
    IOnboardingPresenter {

    override val indexesToShow = listOf(0, 1)

    override val headline: String = "mobile.onboarding.fullMode.headline".i18n()

    override fun evaluateButtonText(deviceSettings: Settings?): String {
        return "mobile.onboarding.createProfile".i18n()
    }
}
