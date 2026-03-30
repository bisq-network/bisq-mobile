package network.bisq.mobile.presentation.startup.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun SplashScreen() {
    val presenter: SplashPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        BisqStaticScaffold(
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                BisqLogoGrey(modifier = Modifier.size(155.dp))
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.BaseLight(
                    text = uiState.appNameAndVersion,
                    color = BisqTheme.colors.mid_grey20,
                    modifier = Modifier.padding(bottom = 20.dp),
                )

                BisqProgressBar(uiState.progress)

                BisqText.BaseRegularGrey(
                    text = uiState.status,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when (uiState.activeDialog) {
            SplashActiveDialog.TimeoutIos -> {
                WarningConfirmationDialog(
                    headline = "mobile.bootstrap.timeout.title".i18n(),
                    message = "mobile.bootstrap.timeout.message.ios".i18n(uiState.currentBootstrapStage),
                    confirmButtonText = "mobile.bootstrap.timeout.continue".i18n(),
                    dismissButtonText = "",
                    onConfirm = { presenter.onAction(SplashUiAction.OnTimeoutDialogContinue) },
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
                    onConfirm = { presenter.onAction(SplashUiAction.OnRestartApp) },
                    onDismiss = { presenter.onAction(SplashUiAction.OnTimeoutDialogContinue) },
                    dismissOnClickOutside = false,
                )
            }

            SplashActiveDialog.TorBootstrapFailed -> {
                WarningConfirmationDialog(
                    headline = "mobile.bootstrap.tor.failed.title".i18n(),
                    message = "mobile.bootstrap.tor.failed.message".i18n(),
                    confirmButtonText = "mobile.bootstrap.tor.failed.purgeRestart".i18n(),
                    dismissButtonText = "mobile.bootstrap.tor.failed.restart".i18n(),
                    onConfirm = { presenter.onAction(SplashUiAction.OnPurgeRestartTor) },
                    onDismiss = { presenter.onAction(SplashUiAction.OnRestartTor) },
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
                    onConfirm = { presenter.onAction(SplashUiAction.OnRestartApp) },
                    onDismiss = { presenter.onAction(SplashUiAction.OnTerminateApp) },
                    dismissOnClickOutside = false,
                )
            }

            null -> Unit
        }
    }
}
