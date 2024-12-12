package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter

class NodeSplashPresenter(
    mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository
) : SplashPresenter(mainPresenter, applicationBootstrapFacade, userProfileService, settingsRepository) {

    /**
     * 
     */
    override fun doCustomNavigationLogic(settings: Settings): Boolean {
        // do nothin
        return false
    }
}
