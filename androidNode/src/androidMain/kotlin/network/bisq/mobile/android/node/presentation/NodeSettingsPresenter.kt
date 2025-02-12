package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter

class NodeSettingsPresenter(
    settingsRepository: SettingsRepository,
    mainPresenter: MainPresenter): SettingsPresenter(settingsRepository, mainPresenter), ISettingsPresenter {

    override val appName: String = BuildNodeConfig.APP_NAME

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        return menuItems.toList()
    }

    override fun versioning(): Triple<String, String, String> {
        val version = BuildNodeConfig.APP_VERSION
        val bisqCoreVersion = BuildNodeConfig.BISQ_CORE_VERSION
        return Triple(version, "core", bisqCoreVersion)
    }
}