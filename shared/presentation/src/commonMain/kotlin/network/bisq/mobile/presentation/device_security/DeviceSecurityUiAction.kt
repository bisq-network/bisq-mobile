package network.bisq.mobile.presentation.device_security

sealed interface DeviceSecurityUiAction {
    /** The dashboard entered composition. Runs the check and decides whether the warning shows. */
    data object OnDashboardArrival : DeviceSecurityUiAction

    /** Swipe, scrim, back and "Got it". Closes the sheet for this visit and persists nothing. */
    data object OnDismiss : DeviceSecurityUiAction

    /** The explicit, persisted decision not to show the warning on arrival again. */
    data object OnDontShowAgain : DeviceSecurityUiAction

    /** Tap on the Settings row. */
    data object OnOpenFromSettings : DeviceSecurityUiAction
}
