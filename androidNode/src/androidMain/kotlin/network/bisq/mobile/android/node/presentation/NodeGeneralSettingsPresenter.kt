package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.GeneralSettingsPresenter

class NodeGeneralSettingsPresenter(
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter
) : GeneralSettingsPresenter(settingsRepository, settingsServiceFacade, languageServiceFacade, mainPresenter) {

    override val shouldShowPoWAdjustmentFactor: StateFlow<Boolean> = MutableStateFlow(true)

}