package network.bisq.mobile.presentation.settings.support

sealed interface SupportUiAction {
    data object OnOpenSupportChannel : SupportUiAction

    data object OnRestartApp : SupportUiAction

    data object OnTerminateApp : SupportUiAction
}
