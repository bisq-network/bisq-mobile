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
    /**
     * Set by the presenter from the platform — true on Connect (Android client
     * + iOS), false on Node app where push notifications go through the
     * embedded local foreground service rather than the relay.
     */
    val shouldShowPushNotificationsToggle: Boolean = true,
    val pushNotificationsEnabled: Boolean = false,
    /**
     * Set by the presenter from the platform — true ONLY on Android Connect.
     * The "keep connected in background" sub-setting controls the local
     * foreground service, which is an Android-only concept. iOS has no
     * equivalent mechanism for keeping the WebSocket alive in background,
     * so the toggle is suppressed there even though relayed push is supported.
     * The Node app has no relayed mode so the toggle isn't relevant there either.
     */
    val shouldShowKeepConnectedToggle: Boolean = false,
    val keepConnectedInBackground: Boolean = false,
)
