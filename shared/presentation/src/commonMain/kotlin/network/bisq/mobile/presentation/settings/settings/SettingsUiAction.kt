package network.bisq.mobile.presentation.settings.settings

sealed interface SettingsUiAction {
    data class OnLanguageCodeChange(
        val langCode: String,
    ) : SettingsUiAction

    data class OnSupportedLanguageCodeToggle(
        val key: String,
        val selected: Boolean,
    ) : SettingsUiAction

    data class OnCloseOfferWhenTradeTakenChange(
        val value: Boolean,
    ) : SettingsUiAction

    data class OnTradePriceToleranceChange(
        val value: String,
    ) : SettingsUiAction

    data class OnTradePriceToleranceFocus(
        val hasFocus: Boolean,
    ) : SettingsUiAction

    data object OnTradePriceToleranceSave : SettingsUiAction

    data object OnTradePriceToleranceCancel : SettingsUiAction

    data class OnUseAnimationsChange(
        val value: Boolean,
    ) : SettingsUiAction

    data class OnNumDaysAfterRedactingTradeDataChange(
        val value: String,
    ) : SettingsUiAction

    data class OnNumDaysAfterRedactingTradeDataFocus(
        val hasFocus: Boolean,
    ) : SettingsUiAction

    data object OnNumDaysAfterRedactingTradeDataSave : SettingsUiAction

    data object OnNumDaysAfterRedactingTradeDataCancel : SettingsUiAction

    data class OnPowFactorChange(
        val value: String,
    ) : SettingsUiAction

    data class OnPowFactorFocus(
        val hasFocus: Boolean,
    ) : SettingsUiAction

    data object OnPowFactorSave : SettingsUiAction

    data object OnPowFactorCancel : SettingsUiAction

    data class OnIgnorePowChange(
        val value: Boolean,
    ) : SettingsUiAction

    data object OnRetryLoadSettingsClick : SettingsUiAction
}
