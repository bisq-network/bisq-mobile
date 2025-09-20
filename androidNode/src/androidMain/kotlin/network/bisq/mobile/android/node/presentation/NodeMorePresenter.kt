package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MorePresenter

class NodeMorePresenter(
    settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : MorePresenter(settingsRepository, userProfileService, mainPresenter), ISettingsPresenter {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        return menuItems.toList()
    }
}