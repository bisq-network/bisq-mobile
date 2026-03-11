package network.bisq.mobile.presentation.settings.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.settings.DEFAULT_MAX_TRADE_PRICE_DEVIATION
import network.bisq.mobile.domain.data.replicated.settings.DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA
import network.bisq.mobile.domain.formatters.NumberFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.DEFAULT_LANGUAGE_CODE
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.base.SnackbarPosition
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.main.MainPresenter

open class SettingsPresenter(
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                tradePriceTolerance = createTradePriceToleranceDataEntry(NumberFormatter.format(DEFAULT_MAX_TRADE_PRICE_DEVIATION * 100)),
                numDaysAfterRedactingTradeData = createNumDaysAfterRedactingTradeDataDataEntry(DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA.toString()),
                powFactor = createPowFactorDataEntry(DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR.toString()),
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    open val shouldShowPoWAdjustmentFactor = false

    // Store original values from fetchSettings for cancel operations (raw numeric values)
    private var originalMaxTradePriceDeviation: Double = DEFAULT_MAX_TRADE_PRICE_DEVIATION
    private var originalNumDaysAfterRedactingTradeData: Int = DEFAULT_NUM_DAYS_AFTER_REDACTING_TRADE_DATA
    private var originalDifficultyAdjustmentFactor: Double = DEFAULT_DIFFICULTY_ADJUSTMENT_FACTOR

    override fun onViewAttached() {
        super.onViewAttached()
        fetchSettings()
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.OnLanguageCodeChange -> setLanguageCode(action.langCode)
            is SettingsUiAction.OnSupportedLanguageCodeToggle -> {
                setSupportedLanguageCodes(action.key, action.selected)
            }
            is SettingsUiAction.OnCloseOfferWhenTradeTakenChange -> setCloseOfferWhenTradeTaken(action.value)
            is SettingsUiAction.OnTradePriceToleranceChange -> onTradePriceToleranceChange(action.value)
            is SettingsUiAction.OnTradePriceToleranceFocus -> onTradePriceToleranceFocus(action.hasFocus)
            SettingsUiAction.OnTradePriceToleranceSave -> onTradePriceToleranceSave()
            SettingsUiAction.OnTradePriceToleranceCancel -> onTradePriceToleranceCancel()
            is SettingsUiAction.OnUseAnimationsChange -> setUseAnimations(action.value)
            is SettingsUiAction.OnNumDaysAfterRedactingTradeDataChange ->
                onNumDaysAfterRedactingTradeDataChange(action.value)

            is SettingsUiAction.OnNumDaysAfterRedactingTradeDataFocus ->
                onNumDaysAfterRedactingTradeDataFocus(action.hasFocus)

            SettingsUiAction.OnNumDaysAfterRedactingTradeDataSave -> onNumDaysAfterRedactingTradeDataSave()
            SettingsUiAction.OnNumDaysAfterRedactingTradeDataCancel -> onNumDaysAfterRedactingTradeDataCancel()
            is SettingsUiAction.OnPowFactorChange -> onPowFactorChange(action.value)
            is SettingsUiAction.OnPowFactorFocus -> onPowFactorFocus(action.hasFocus)
            SettingsUiAction.OnPowFactorSave -> onPowFactorSave()
            SettingsUiAction.OnPowFactorCancel -> onPowFactorCancel()
            is SettingsUiAction.OnIgnorePowChange -> setIgnorePow(action.value)
            SettingsUiAction.OnRetryLoadSettingsClick -> fetchSettings()
        }
    }

    private fun setLanguageCode(langCode: String) {
        presenterScope.launch {
            showLoading()
            settingsServiceFacade
                .setLanguageCode(langCode)
                .onSuccess {
                    try {
                        setDefaultLocale(langCode)
                        _uiState.update { it.copy(languageCode = langCode) }
                    } catch (e: Exception) {
                        showSnackbar(e.message ?: "mobile.error.generic".i18n(), SnackbarType.ERROR)
                    }
                }.onFailure { exception ->
                    handleError(exception)
                }
            hideLoading()
        }
    }

    private fun setSupportedLanguageCodes(
        langCode: String,
        selected: Boolean,
    ) {
        presenterScope.launch {
            val current = _uiState.value.supportedLanguageCodes
            val next = if (selected) current + langCode else current - langCode
            _uiState.update { it.copy(supportedLanguageCodes = next) }

            showLoading()
            settingsServiceFacade
                .setSupportedLanguageCodes(next)
                .onFailure { exception ->
                    _uiState.update { it.copy(supportedLanguageCodes = current) }
                    handleError(exception, position = SnackbarPosition.TOP)
                }
            hideLoading()
        }
    }

    private fun setCloseOfferWhenTradeTaken(value: Boolean) {
        presenterScope.launch {
            showLoading()
            _uiState.update { it.copy(closeOfferWhenTradeTaken = value) }
            settingsServiceFacade
                .setCloseMyOfferWhenTaken(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(closeOfferWhenTradeTaken = !value) }
                    handleError(exception)
                }
            hideLoading()
        }
    }

    private fun onTradePriceToleranceChange(value: String) {
        _uiState.update {
            val newEntry = it.tradePriceTolerance.updateValue(value)
            val parsedValue = newEntry.value.toDoubleOrNullLocaleAware()
            val hasChanges = parsedValue != null && parsedValue != originalMaxTradePriceDeviation * 100
            it.copy(
                tradePriceTolerance = newEntry,
                hasChangesTradePriceTolerance = hasChanges,
            )
        }
    }

    private fun onTradePriceToleranceFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(tradePriceTolerance = it.tradePriceTolerance.validate())
            }
        }
    }

    private fun onTradePriceToleranceSave() {
        presenterScope.launch {
            _uiState.update {
                it.copy(tradePriceTolerance = it.tradePriceTolerance.validate())
            }
            val currentEntry = _uiState.value.tradePriceTolerance
            if (currentEntry.isValid) {
                val parsedValue = currentEntry.value.toDoubleOrNullLocaleAware()
                if (parsedValue != null) {
                    val newDeviation = parsedValue / 100
                    settingsServiceFacade.setMaxTradePriceDeviation(newDeviation)
                    // Update original value and reset hasChanges after successful save
                    originalMaxTradePriceDeviation = newDeviation
                    _uiState.update {
                        it.copy(hasChangesTradePriceTolerance = false)
                    }
                }
            }
        }
    }

    private fun onTradePriceToleranceCancel() {
        val formattedValue = NumberFormatter.format(originalMaxTradePriceDeviation * 100)
        _uiState.update {
            it.copy(
                tradePriceTolerance = it.tradePriceTolerance.updateValue(formattedValue),
                hasChangesTradePriceTolerance = false,
            )
        }
    }

    private fun onNumDaysAfterRedactingTradeDataChange(value: String) {
        _uiState.update {
            val newEntry = it.numDaysAfterRedactingTradeData.updateValue(value)
            val parsedValue = newEntry.value.toIntOrNull()
            val hasChanges = parsedValue != null && parsedValue != originalNumDaysAfterRedactingTradeData
            it.copy(
                numDaysAfterRedactingTradeData = newEntry,
                hasChangesNumDaysAfterRedactingTradeData = hasChanges,
            )
        }
    }

    private fun onNumDaysAfterRedactingTradeDataFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.validate())
            }
        }
    }

    private fun onNumDaysAfterRedactingTradeDataSave() {
        presenterScope.launch {
            _uiState.update {
                it.copy(numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.validate())
            }
            val currentEntry = _uiState.value.numDaysAfterRedactingTradeData
            if (currentEntry.isValid) {
                val parsedValue = currentEntry.value.toIntOrNull()
                if (parsedValue != null) {
                    settingsServiceFacade.setNumDaysAfterRedactingTradeData(parsedValue)
                    // Update original value and reset hasChanges after successful save
                    originalNumDaysAfterRedactingTradeData = parsedValue
                    _uiState.update {
                        it.copy(hasChangesNumDaysAfterRedactingTradeData = false)
                    }
                }
            }
        }
    }

    private fun onNumDaysAfterRedactingTradeDataCancel() {
        _uiState.update {
            it.copy(
                numDaysAfterRedactingTradeData =
                    it.numDaysAfterRedactingTradeData.updateValue(
                        originalNumDaysAfterRedactingTradeData.toString(),
                    ),
                hasChangesNumDaysAfterRedactingTradeData = false,
            )
        }
    }

    private fun setUseAnimations(value: Boolean) {
        presenterScope.launch {
            showLoading()
            _uiState.update { it.copy(useAnimations = value) }
            settingsServiceFacade
                .setUseAnimations(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(useAnimations = !value) }
                    handleError(exception)
                }
            hideLoading()
        }
    }

    private fun onPowFactorChange(value: String) {
        _uiState.update {
            val newEntry = it.powFactor.updateValue(value)
            val parsedValue = newEntry.value.toDoubleOrNullLocaleAware()
            val hasChanges = parsedValue != null && parsedValue != originalDifficultyAdjustmentFactor
            it.copy(
                powFactor = newEntry,
                hasChangesPowFactor = hasChanges,
            )
        }
    }

    private fun onPowFactorFocus(hasFocus: Boolean) {
        if (!hasFocus) {
            _uiState.update {
                it.copy(powFactor = it.powFactor.validate())
            }
        }
    }

    private fun onPowFactorSave() {
        presenterScope.launch {
            _uiState.update {
                it.copy(powFactor = it.powFactor.validate())
            }
            val currentEntry = _uiState.value.powFactor
            if (currentEntry.isValid) {
                currentEntry.value.toDoubleOrNullLocaleAware()?.let { parsedValue ->
                    settingsServiceFacade.setDifficultyAdjustmentFactor(parsedValue)
                    // Update original value and reset hasChanges after successful save
                    originalDifficultyAdjustmentFactor = parsedValue
                    _uiState.update {
                        it.copy(hasChangesPowFactor = false)
                    }
                }
            }
        }
    }

    private fun onPowFactorCancel() {
        _uiState.update {
            it.copy(
                powFactor = it.powFactor.updateValue(originalDifficultyAdjustmentFactor.toString()),
                hasChangesPowFactor = false,
            )
        }
    }

    private fun setIgnorePow(value: Boolean) {
        presenterScope.launch {
            showLoading()
            _uiState.update { it.copy(ignorePow = value) }
            settingsServiceFacade
                .setIgnoreDiffAdjustmentFromSecManager(value)
                .onFailure { exception ->
                    _uiState.update { it.copy(ignorePow = !value) }
                    handleError(exception)
                }
            hideLoading()
        }
    }

    private fun fetchSettings() {
        presenterScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingSettings = true,
                    isFetchingSettingsError = false,
                )
            }

            settingsServiceFacade
                .getSettings()
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isFetchingSettings = false,
                            isFetchingSettingsError = true,
                        )
                    }
                }.onSuccess { settings ->
                    val supportedLangCodes = settings.supportedLanguageCodes.ifEmpty { setOf(DEFAULT_LANGUAGE_CODE) }
                    val tradePriceToleranceFormatted = NumberFormatter.format(settings.maxTradePriceDeviation * 100)
                    val numDaysFormatted = settings.numDaysAfterRedactingTradeData.toString()
                    val powFactorFormatted = settingsServiceFacade.difficultyAdjustmentFactor.value.toString()
                    val ignorePowValue = settingsServiceFacade.ignoreDiffAdjustmentFromSecManager.value

                    // Store original values for cancel operations (raw numeric values from settings)
                    originalMaxTradePriceDeviation = settings.maxTradePriceDeviation
                    originalNumDaysAfterRedactingTradeData = settings.numDaysAfterRedactingTradeData
                    originalDifficultyAdjustmentFactor = settingsServiceFacade.difficultyAdjustmentFactor.value

                    _uiState.update {
                        it.copy(
                            i18nPairs = languageServiceFacade.i18nPairs.value,
                            allLanguagePairs = languageServiceFacade.allPairs.value,
                            languageCode = settings.languageCode,
                            supportedLanguageCodes = supportedLangCodes,
                            closeOfferWhenTradeTaken = settings.closeMyOfferWhenTaken,
                            tradePriceTolerance = it.tradePriceTolerance.updateValue(tradePriceToleranceFormatted),
                            useAnimations = settings.useAnimations,
                            numDaysAfterRedactingTradeData = it.numDaysAfterRedactingTradeData.updateValue(numDaysFormatted),
                            powFactor = it.powFactor.updateValue(powFactorFormatted),
                            ignorePow = ignorePowValue,
                            shouldShowPoWAdjustmentFactor = shouldShowPoWAdjustmentFactor,
                            isFetchingSettings = false,
                            isFetchingSettingsError = false,
                            // Reset hasChanges flags since we just loaded original values
                            hasChangesTradePriceTolerance = false,
                            hasChangesNumDaysAfterRedactingTradeData = false,
                            hasChangesPowFactor = false,
                        )
                    }
                }
        }
    }
}

private fun createTradePriceToleranceDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toDoubleOrNullLocaleAware()
                if (parsedValue == null || parsedValue < 1 || parsedValue > 10) {
                    "settings.trade.maxTradePriceDeviation.invalid".i18n(1, 10)
                } else {
                    null
                }
            }
        },
    )

private fun createNumDaysAfterRedactingTradeDataDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toIntOrNull()
                if (parsedValue == null || parsedValue < 30 || parsedValue > 365) {
                    "settings.trade.numDaysAfterRedactingTradeData.invalid".i18n(30, 365)
                } else {
                    null
                }
            }
        },
    )

private fun createPowFactorDataEntry(value: String): DataEntry =
    DataEntry(
        value = value,
        validator = { it ->
            if (it.isEmpty()) {
                "mobile.validation.valueCannotBeEmpty".i18n()
            } else {
                val parsedValue = it.toDoubleOrNullLocaleAware()
                if (parsedValue == null || parsedValue < 0 || parsedValue > 160_000) {
                    "authorizedRole.securityManager.difficultyAdjustment.invalid".i18n(160000)
                } else {
                    null
                }
            }
        },
    )
