package network.bisq.mobile.presentation.startup.splash

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog

@Composable
fun SplashDialogs(
    uiState: SplashUiState,
    onAction: (SplashUiAction) -> Unit,
) {
    when (uiState.activeDialog) {
        SplashActiveDialog.TimeoutIos -> {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.timeout.title".i18n(),
                message = "mobile.bootstrap.timeout.message.ios".i18n(uiState.currentBootstrapStage),
                confirmButtonText = "mobile.bootstrap.timeout.continue".i18n(),
                dismissButtonText = "",
                onConfirm = { onAction(SplashUiAction.OnTimeoutDialogContinue) },
                onDismiss = { },
                dismissOnClickOutside = false,
            )
        }

        SplashActiveDialog.TimeoutAndroid -> {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.timeout.title".i18n(),
                message = "mobile.bootstrap.timeout.message".i18n(uiState.currentBootstrapStage),
                confirmButtonText = "mobile.bootstrap.timeout.restart".i18n(),
                dismissButtonText = "mobile.bootstrap.timeout.continue".i18n(),
                onConfirm = { onAction(SplashUiAction.OnRestartApp) },
                onDismiss = { onAction(SplashUiAction.OnTimeoutDialogContinue) },
                dismissOnClickOutside = false,
            )
        }

        SplashActiveDialog.TorBootstrapFailed -> {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.tor.failed.title".i18n(),
                message = "mobile.bootstrap.tor.failed.message".i18n(),
                confirmButtonText = "mobile.bootstrap.tor.failed.purgeRestart".i18n(),
                dismissButtonText = "mobile.bootstrap.tor.failed.restart".i18n(),
                onConfirm = { onAction(SplashUiAction.OnPurgeRestartTor) },
                onDismiss = { onAction(SplashUiAction.OnRestartTor) },
                verticalButtonPlacement = true,
                dismissOnClickOutside = false,
            )
        }

        SplashActiveDialog.BootstrapFailedIos -> {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.failed.title".i18n(),
                message = "mobile.bootstrap.failed.message".i18n(uiState.currentBootstrapStage),
                confirmButtonText = "",
                dismissButtonText = "",
                onConfirm = { },
                onDismiss = { },
                dismissOnClickOutside = false,
            )
        }

        SplashActiveDialog.BootstrapFailedAndroid -> {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.failed.title".i18n(),
                message = "mobile.bootstrap.failed.message".i18n(uiState.currentBootstrapStage),
                confirmButtonText = "mobile.bootstrap.failed.restart".i18n(),
                dismissButtonText = "mobile.bootstrap.failed.shutdown".i18n(),
                onConfirm = { onAction(SplashUiAction.OnRestartApp) },
                onDismiss = { onAction(SplashUiAction.OnTerminateApp) },
                dismissOnClickOutside = false,
            )
        }

        null -> Unit
    }
}
