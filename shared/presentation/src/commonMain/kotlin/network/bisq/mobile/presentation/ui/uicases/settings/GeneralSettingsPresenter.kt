package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.collection.MutableScatterMap
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PaymentAccount

open class GeneralSettingsPresenter(
    private val settingsRepository: SettingsRepository,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IGeneralSettingsPresenter {

    override val i18nCodes: StateFlow<List<String>> = languageServiceFacade.i18nCodes

    private val _selectedLanguage: MutableStateFlow<String> = MutableStateFlow("en")
    override val selectedLanguage: MutableStateFlow<String> = _selectedLanguage

    override fun selectLanguage(langCode: String) {
        _selectedLanguage.value = langCode
    }


}