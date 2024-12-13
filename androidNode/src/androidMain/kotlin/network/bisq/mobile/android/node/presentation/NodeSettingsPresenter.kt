package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MenuItem
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter

class NodeSettingsPresenter(
    settingsRepository: SettingsRepository,
    mainPresenter: MainPresenter): SettingsPresenter(settingsRepository, mainPresenter), ISettingsPresenter {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        return menuItems.toList()
    }
}