package network.bisq.mobile.presentation.settings.settings

import network.bisq.mobile.i18n.DEFAULT_LANGUAGE_CODE
import network.bisq.mobile.presentation.common.ui.utils.DataEntry

data class SettingsUiState(
    val i18nPairs: Map<String, String> = emptyMap(),
    val allLanguagePairs: Map<String, String> = emptyMap(),
    val languageCode: String = DEFAULT_LANGUAGE_CODE,
    val supportedLanguageCodes: Set<String> = setOf(DEFAULT_LANGUAGE_CODE),
    val closeOfferWhenTradeTaken: Boolean = true,
    val tradePriceTolerance: DataEntry,
    val numDaysAfterRedactingTradeData: DataEntry,
    val powFactor: DataEntry,
    val ignorePow: Boolean = false,
    val shouldShowPoWAdjustmentFactor: Boolean = false,
    val useAnimations: Boolean = true,
    val isFetchingSettings: Boolean = true,
    val isFetchingSettingsError: Boolean = false,
    val hasChangesTradePriceTolerance: Boolean = false,
    val hasChangesNumDaysAfterRedactingTradeData: Boolean = false,
    val hasChangesPowFactor: Boolean = false,
)
