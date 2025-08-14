package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class GeneralSettingsPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IGeneralSettingsPresenter {

    override val i18nPairs: StateFlow<List<Pair<String, String>>> get() = languageServiceFacade.i18nPairs
    override val allLanguagePairs: StateFlow<List<Pair<String, String>>> get() = languageServiceFacade.allPairs

    private val _languageCode: MutableStateFlow<String> = MutableStateFlow("en")
    override val languageCode: StateFlow<String> get() = _languageCode.asStateFlow()
    override fun setLanguageCode(langCode: String) {
        disableInteractive()
        launchUI {
            try {
                println("i18n :: GeneralSettingsPresenter :: setLanguageCode :: $langCode")
                setDefaultLocale(langCode) // update lang in app's context
                settingsServiceFacade.setLanguageCode(langCode) // Update lang in bisq2 lib / WS
                // Doing this to reload all bundles of the newly selected language,
                // all String.i18n() across the app gets the text of selected language
                I18nSupport.initialize(langCode) // update lang for mobile's i18n libs
                _languageCode.value = langCode

                // As per chat with @Henrik Feb 4, it's okay not to translate `supported languages` lists into selected languages, for now.
                // To update display values in i18Pairs, allLanguagePairs with the new language
                // languageServiceFacade.setDefaultLanguage(langCode)
                // languageServiceFacade.sync()
            } finally {
                enableInteractive()
            }
        }
    }

    private val _supportedLanguageCodes: MutableStateFlow<Set<String>> = MutableStateFlow(setOf("en"))
    override val supportedLanguageCodes: StateFlow<Set<String>> get() = _supportedLanguageCodes.asStateFlow()
    override fun setSupportedLanguageCodes(langCodes: Set<String>) {
        disableInteractive()
        launchUI {
            try {
                _supportedLanguageCodes.value = langCodes
                settingsServiceFacade.setSupportedLanguageCodes(langCodes)
            } finally {
                enableInteractive()
            }
        }
    }

    private val _chatNotification: MutableStateFlow<String> =
        MutableStateFlow("chat.notificationsSettingsMenu.all".i18n())
    override val chatNotification: StateFlow<String> get() = _chatNotification.asStateFlow()
    override fun setChatNotification(value: String) {
        disableInteractive()
        launchUI {
            try {
                _chatNotification.value = value
            } finally {
                enableInteractive()
            }
        }
    }

    private val _closeOfferWhenTradeTaken: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val closeOfferWhenTradeTaken: StateFlow<Boolean> get() = _closeOfferWhenTradeTaken.asStateFlow()
    override fun setCloseOfferWhenTradeTaken(value: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _closeOfferWhenTradeTaken.value = value
                settingsServiceFacade.setCloseMyOfferWhenTaken(value)
            } finally {
                enableInteractive()
            }
        }
    }

    // This is internally represented as ratio. So 100% is saved as 1.0, 5% as 0.05.
    // Hence the 100 multiplier and divider
    private val _tradePriceTolerance: MutableStateFlow<String> = MutableStateFlow("5")
    override val tradePriceTolerance: StateFlow<String> get() = _tradePriceTolerance.asStateFlow()
    override fun setTradePriceTolerance(value: String, isValid: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _tradePriceTolerance.value = value
                if (isValid) {
                    val _value = value.toDoubleOrNullLocaleAware()
                    if (_value != null) {
                        settingsServiceFacade.setMaxTradePriceDeviation(_value / 100)
                    }
                }
            } finally {
                enableInteractive()
            }
        }
    }

    private val _numDaysAfterRedactingTradeData = MutableStateFlow("90")
    override val numDaysAfterRedactingTradeData: StateFlow<String> get() = _numDaysAfterRedactingTradeData.asStateFlow()
    override fun setNumDaysAfterRedactingTradeData(value: String, isValid: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _numDaysAfterRedactingTradeData.value = value
                if (isValid) {
                    val _value = value.toIntOrNull()
                    if (_value != null) {
                        settingsServiceFacade.setNumDaysAfterRedactingTradeData(_value)
                    }
                }
            } finally {
                enableInteractive()
            }
        }
    }

    private val _useAnimations: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val useAnimations: StateFlow<Boolean> get() = _useAnimations.asStateFlow()
    override fun setUseAnimations(value: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _useAnimations.value = value
                settingsServiceFacade.setUseAnimations(value)
            } finally {
                enableInteractive()
            }
        }
    }

    private val _powFactor: MutableStateFlow<String> = MutableStateFlow("1")
    override val powFactor: StateFlow<String> get() = _powFactor.asStateFlow()
    override fun setPowFactor(value: String, isValid: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _powFactor.value = value
                if (isValid) {
                    val _value = value.toIntOrNull()
                    settingsServiceFacade.setDifficultyAdjustmentFactor(_value?.toDouble() ?: 0.0)
                }
            } finally {
                enableInteractive()
            }
        }
    }

    private val _ignorePow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ignorePow: StateFlow<Boolean> get() = _ignorePow.asStateFlow()
    override fun setIgnorePow(value: Boolean) {
        disableInteractive()
        launchUI {
            try {
                _ignorePow.value = value
                settingsServiceFacade.setIgnoreDiffAdjustmentFromSecManager(value)
            } finally {
                enableInteractive()
            }
        }
    }

    override val shouldShowPoWAdjustmentFactor: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    private var jobs: MutableSet<Job> = mutableSetOf()

    init {
        collectUI(mainPresenter.languageCode.drop(1)) {
            fetchSettings()
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            fetchSettings()
        }
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        super.onViewUnattaching()
    }

    private suspend fun fetchSettings() {
        val settings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
        _languageCode.value = settings.languageCode
        _supportedLanguageCodes.value = if (settings.supportedLanguageCodes.isNotEmpty())
            settings.supportedLanguageCodes
        else
            setOf("en")
        _closeOfferWhenTradeTaken.value = settings.closeMyOfferWhenTaken
        _tradePriceTolerance.value = NumberFormatter.format(settings.maxTradePriceDeviation * 100)
        _useAnimations.value = settings.useAnimations
        _numDaysAfterRedactingTradeData.value = settings.numDaysAfterRedactingTradeData.toString()
        _powFactor.value = settingsServiceFacade.difficultyAdjustmentFactor.value.toInt().toString()
        _ignorePow.value = settingsServiceFacade.ignoreDiffAdjustmentFromSecManager.value
    }
}