package network.bisq.mobile.presentation.startup.splash

sealed interface SplashUiAction {
    data object OnTimeoutDialogContinue : SplashUiAction

    data object OnRestartApp : SplashUiAction

    data object OnRestartTor : SplashUiAction

    data object OnPurgeRestartTor : SplashUiAction

    data object OnTerminateApp : SplashUiAction
}
