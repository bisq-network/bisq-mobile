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

    /**
     * User tapped the animations toggle while it is greyed out because the device is
     * low-spec ([AnimationSettings.lockedByDevice]). The switch itself stays disabled;
     * this only surfaces an explanation so the greyed state isn't a dead end.
     */
    data object OnUseAnimationsLockedTap : SettingsUiAction

    data class OnRememberOfferbookFilterPreferencesChange(
        val enabled: Boolean,
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

    data object OnResetAllDontShowAgainClick : SettingsUiAction

    data object OnRetryLoadSettingsClick : SettingsUiAction

    /** User toggled the relayed-push-notifications opt-in. */
    data class OnPushNotificationsToggle(
        val enabled: Boolean,
    ) : SettingsUiAction

    /** User tapped a "Learn more" affordance in the relayed-push-notifications section. */
    data object OnPushNotificationsLearnMore : SettingsUiAction

    /**
     * User toggled the "keep connected in background" sub-setting.
     *
     * Only emitted when relayed push notifications are already enabled — the toggle
     * is hidden otherwise. See [shouldShowKeepConnectedToggle] gating in the UiState
     * for the platform + parent-toggle visibility rules.
     */
    data class OnKeepConnectedInBackgroundToggle(
        val enabled: Boolean,
    ) : SettingsUiAction

    /** User toggled the opt-in analytics switch (issue #525). */
    data class OnAnalyticsToggle(
        val enabled: Boolean,
    ) : SettingsUiAction

    /** User tapped "Learn more" in the analytics section — opens the privacy wiki page. */
    data object OnAnalyticsLearnMore : SettingsUiAction
}
