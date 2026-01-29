package network.bisq.mobile.client.onboarding

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.onboarding.OnboardingPresenter

class ClientOnboardingPresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService) {
    override val indexesToShow = listOf(0, 2)

    override val headline: String = "mobile.onboarding.clientMode.headline".i18n()
}
