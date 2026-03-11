package network.bisq.mobile.node.settings.settings

import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter

class NodeSettingsPresenter(
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter,
) : SettingsPresenter(settingsServiceFacade, languageServiceFacade, mainPresenter) {
    override val shouldShowPoWAdjustmentFactor: Boolean
        get() = true
}
