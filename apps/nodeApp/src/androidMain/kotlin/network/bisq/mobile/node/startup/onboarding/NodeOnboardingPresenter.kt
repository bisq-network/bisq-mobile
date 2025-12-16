package network.bisq.mobile.node.startup.onboarding

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.onboarding.IOnboardingPresenter
import network.bisq.mobile.presentation.startup.onboarding.OnboardingPresenter

class NodeOnboardingPresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService),
    IOnboardingPresenter {

    override val indexesToShow = listOf(0, 1)

    override val headline: String = "mobile.onboarding.fullMode.headline".i18n()
}