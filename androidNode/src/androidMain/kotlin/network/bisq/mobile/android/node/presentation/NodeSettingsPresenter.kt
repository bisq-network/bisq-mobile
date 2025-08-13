package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter

class NodeSettingsPresenter(
    settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : SettingsPresenter(settingsRepository, userProfileService, mainPresenter), ISettingsPresenter {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(MenuItem.Leaf(label = "mobile.settings.about".i18n(), route = Routes.About))
        return menuItems.toList()
    }
}