package network.bisq.mobile.presentation.startup.splash

data class SplashUiState(
    val progress: Float = 0f,
    val status: String = "",
    val appNameAndVersion: String = "",
    val currentBootstrapStage: String = "",
    val activeDialog: SplashActiveDialog? = null,
)

sealed interface SplashActiveDialog {
    data object TimeoutIos : SplashActiveDialog

    data object TimeoutAndroid : SplashActiveDialog

    data object TorBootstrapFailed : SplashActiveDialog

    data object BootstrapFailedIos : SplashActiveDialog

    data object BootstrapFailedAndroid : SplashActiveDialog
}
