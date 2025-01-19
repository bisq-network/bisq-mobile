package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.collection.MutableScatterMap
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PaymentAccount

open class GeneralSettingsPresenter(
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IGeneralSettingsPresenter {

    // private val _i18nCodes: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val i18nCodes: StateFlow<List<String>> = languageServiceFacade.i18nCodes

    private val _selectedLanguage: MutableStateFlow<String> = MutableStateFlow("en")
    override val languageCode: MutableStateFlow<String> = _selectedLanguage
    override fun setLanguageCode(langCode: String) {
        backgroundScope.launch {
            _selectedLanguage.value = langCode
            settingsServiceFacade.setLanguageCode(langCode)
        }
    }

    private val _preferredLanguages: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    override val supportedLanguageCodes: MutableStateFlow<Set<String>> = _preferredLanguages
    override fun setSupportedLanguageCodes(langCodes: Set<String>) {
        backgroundScope.launch {
            _preferredLanguages.value = langCodes
            settingsServiceFacade.setSupportedLanguageCodes(langCodes)
        }
    }

    private val _tradeNotification: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val tradeNotification: MutableStateFlow<Boolean> = _tradeNotification

    override fun setTradeNotification(value: Boolean) {
        backgroundScope.launch {
            _tradeNotification.value = value
            // settingsServiceFacade.No(langCodes)
        }
    }


    private val _chatNotification: MutableStateFlow<String> =
        MutableStateFlow("chat.notificationsSettingsMenu.all".i18n())
    override val chatNotification: StateFlow<String> = _chatNotification

    override fun setChatNotification(value: String) {
        _chatNotification.value = value
    }

    private val _closeOfferWhenTradeTaken: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val closeOfferWhenTradeTaken: StateFlow<Boolean> = _closeOfferWhenTradeTaken
    override fun setCloseOfferWhenTradeTaken(value: Boolean) {
        backgroundScope.launch {
            _closeOfferWhenTradeTaken.value = value
            settingsServiceFacade.setCloseMyOfferWhenTaken(value)
        }
    }

    private val _tradePriceTolerance: MutableStateFlow<Double> = MutableStateFlow(5.0)
    override val tradePriceTolerance: StateFlow<Double> = _tradePriceTolerance
    override fun setTradePriceTolerance(value: Double) {
        backgroundScope.launch {
            _tradePriceTolerance.value = value
            settingsServiceFacade.setMaxTradePriceDeviation(value)
        }
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> = _useAnimations
    override fun setUseAnimations(value: Boolean) {
        backgroundScope.launch {
            _useAnimations.value = value
            settingsServiceFacade.setUseAnimations(value)
        }
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

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
        jobs.add(backgroundScope.launch {
            val settings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
            _selectedLanguage.value = settings.languageCode
            _preferredLanguages.value = settings.supportedLanguageCodes
            // _tradeNotification.value =
            // _chatNotification.value =
            _closeOfferWhenTradeTaken.value = settings.closeMyOfferWhenTaken
            _tradePriceTolerance.value = settings.maxTradePriceDeviation
            // _useAnimations.value
            // _powFactor.value
            // _ignorePow.value
        })
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

}