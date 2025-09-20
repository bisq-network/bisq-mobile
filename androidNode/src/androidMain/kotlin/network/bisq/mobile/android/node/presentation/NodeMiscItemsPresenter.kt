package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter

class NodeMiscItemsPresenter(
    settingsRepository: SettingsRepository,
    userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : MiscItemsPresenter(settingsRepository, userProfileService, mainPresenter) {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        return menuItems.toList()
    }
}