package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.collection.MutableScatterMap
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.i18n.i18n
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

    private val _tradeNotification: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val tradeNotification: MutableStateFlow<Boolean> = _tradeNotification

    override fun setTradeNotification(value: Boolean) {
        _tradeNotification.value = value
    }


    private val _chatNotification: MutableStateFlow<String> = MutableStateFlow("chat.notificationsSettingsMenu.all".i18n())
    override val chatNotification: StateFlow<String> = _chatNotification

    override fun setChatNotification(value: String) {
        _chatNotification.value = value
    }

    private val _closeOfferWhenTradeTaken: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val closeOfferWhenTradeTaken: StateFlow<Boolean> = _closeOfferWhenTradeTaken
    override fun setCloseOfferWhenTradeTaken(value: Boolean) {
        _closeOfferWhenTradeTaken.value = value
    }

    private val _tradePriceTolerance: MutableStateFlow<String> = MutableStateFlow("5%")
    override val tradePriceTolerance: StateFlow<String> = _tradePriceTolerance
    override fun setTradePriceTolerance(value: String) {
        _tradePriceTolerance.value = value
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> = _useAnimations
    override fun setUseAnimations(value: Boolean) {
        _useAnimations.value = value
    }

    private val _powFactor: MutableStateFlow<String> = MutableStateFlow("1")
    override val powFactor: StateFlow<String> = _powFactor
    override fun setPowFactor(value: String) {
        _powFactor.value = value
    }

    private val _ignorePow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignorePow: StateFlow<Boolean> = _ignorePow
    override fun setIgnorePow(value: Boolean) {
        _ignorePow.value = value
    }
}