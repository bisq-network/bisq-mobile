package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter

class OnBoardingNodePresenter(
    mainPresenter: MainPresenter,
    settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
) : OnBoardingPresenter(mainPresenter, settingsRepository, userProfileService), IOnboardingPresenter {
    override val indexesToShow = listOf(1, 2)

    override fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean) {
        navigateToCreateProfile()
    }
}
