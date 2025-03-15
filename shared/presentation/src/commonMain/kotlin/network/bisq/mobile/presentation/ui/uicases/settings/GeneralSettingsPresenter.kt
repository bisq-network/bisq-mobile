package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class GeneralSettingsPresenter(
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IGeneralSettingsPresenter {

    override val i18nPairs: StateFlow<List<Pair<String, String>>> = languageServiceFacade.i18nPairs
    override val allLanguagePairs: StateFlow<List<Pair<String, String>>> = languageServiceFacade.allPairs

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("en")
    override val languageCode: MutableStateFlow<String> = _languageCode
    override fun setLanguageCode(langCode: String) {
        backgroundScope.launch {
            settingsServiceFacade.setLanguageCode(langCode)
            // TODO: Is this right?
            // Doing this to reload all bundles of the newly selected language,
            // all String.i18n() across the app gets the text of selected language
            I18nSupport.initialize(langCode)
            // As per chat with @Henrik Feb 4, it's okay not to translate these lists into selected languages, for now.
            // To update display values in i18Pairs, allLanguagePairs with the new language
            // languageServiceFacade.setDefaultLanguage(langCode)
            // languageServiceFacade.sync()
            _languageCode.value = langCode
        }
    }

    private val _supportedLanguageCodes: MutableStateFlow<Set<String>> = MutableStateFlow(setOf("en"))
    override val supportedLanguageCodes: MutableStateFlow<Set<String>> = _supportedLanguageCodes
    override fun setSupportedLanguageCodes(langCodes: Set<String>) {
        backgroundScope.launch {
            _supportedLanguageCodes.value = langCodes
            settingsServiceFacade.setSupportedLanguageCodes(langCodes)
        }
    }

    private val _chatNotification: MutableStateFlow<String> =
        MutableStateFlow("chat.notificationsSettingsMenu.all".i18n())
    override val chatNotification: StateFlow<String> = _chatNotification

    override fun setChatNotification(value: String) {
        backgroundScope.launch {
            _chatNotification.value = value
            // settingsServiceFacade.setChatNotificationType(value)
        }
    }

    private val _closeOfferWhenTradeTaken: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val closeOfferWhenTradeTaken: StateFlow<Boolean> = _closeOfferWhenTradeTaken
    override fun setCloseOfferWhenTradeTaken(value: Boolean) {
        backgroundScope.launch {
            _closeOfferWhenTradeTaken.value = value
            settingsServiceFacade.setCloseMyOfferWhenTaken(value)
        }
    }

    // This is internally represented as ratio. So 100% is saved as 1.0, 5% as 0.05.
    // Hence the 100 multiplier and divider
    private val _tradePriceTolerance: MutableStateFlow<String> = MutableStateFlow("5")
    override val tradePriceTolerance: StateFlow<String> = _tradePriceTolerance
    override fun setTradePriceTolerance(value: String, isValid: Boolean) {
        backgroundScope.launch {
            _tradePriceTolerance.value = value
            if (isValid) {
                val _value = value.toDoubleOrNull()
                settingsServiceFacade.setMaxTradePriceDeviation((_value ?: 0.0)/100)
            }
        }
    }

    private val _numDaysAfterRedactingTradeData = MutableStateFlow("90")
    override val numDaysAfterRedactingTradeData: StateFlow<String> = _numDaysAfterRedactingTradeData
    override fun setNumDaysAfterRedactingTradeData(value: String, isValid: Boolean) {
        backgroundScope.launch {
            _numDaysAfterRedactingTradeData.value = value
            if (isValid) {
                val _value = value.toIntOrNull()
                if (_value != null) {
                    settingsServiceFacade.setNumDaysAfterRedactingTradeData(_value)
                }
            }
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
    override fun setPowFactor(value: String, isValid: Boolean) {
        backgroundScope.launch {
            _powFactor.value = value
            if (isValid) {
                val _value = value.toDoubleOrNull()
                settingsServiceFacade.setDifficultyAdjustmentFactor(_value ?: 0.0)
            }
        }
    }

    private val _ignorePow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignorePow: StateFlow<Boolean> = _ignorePow
    override fun setIgnorePow(value: Boolean) {
        backgroundScope.launch {
            _ignorePow.value = value
            settingsServiceFacade.setIgnoreDiffAdjustmentFromSecManager(value)
        }
    }

    override val shouldShowPoWAdjustmentFactor: StateFlow<Boolean> = MutableStateFlow(false)

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
        jobs.add(backgroundScope.launch {
            val settings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
            _languageCode.value = settings.languageCode
            _supportedLanguageCodes.value = if (settings.supportedLanguageCodes.isNotEmpty())
                settings.supportedLanguageCodes
            else
                setOf("en") // setOf(i18nPairs.collectAsState().value.first().first)

            // _chatNotification.value =
            _closeOfferWhenTradeTaken.value = settings.closeMyOfferWhenTaken
            _tradePriceTolerance.value = (settings.maxTradePriceDeviation * 100).toString()
            _useAnimations.value = settings.useAnimations
            _numDaysAfterRedactingTradeData.value = settings.numDaysAfterRedactingTradeData.toString()
            _powFactor.value = settingsServiceFacade.difficultyAdjustmentFactor.value.toString()
            _ignorePow.value = settingsServiceFacade.ignoreDiffAdjustmentFromSecManager.value
        })
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

}