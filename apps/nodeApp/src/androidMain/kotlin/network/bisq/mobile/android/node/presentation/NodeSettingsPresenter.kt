package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter

class NodeSettingsPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter
) : SettingsPresenter(settingsServiceFacade, languageServiceFacade, mainPresenter) {

    private val _shouldShowPoWAdjustmentFactor = MutableStateFlow(true)
    override val shouldShowPoWAdjustmentFactor: StateFlow<Boolean> get() = _shouldShowPoWAdjustmentFactor.asStateFlow()

}