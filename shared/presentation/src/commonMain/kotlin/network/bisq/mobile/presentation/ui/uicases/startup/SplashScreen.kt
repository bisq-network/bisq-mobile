package network.bisq.mobile.presentation.ui.uicases.startup

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
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun SplashScreen() {
    val presenter: SplashPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val progress by presenter.progress.collectAsState()
    val state by presenter.state.collectAsState()
    val isTimeoutDialogVisible by presenter.isTimeoutDialogVisible.collectAsState()
    val isBootstrapFailed by presenter.isBootstrapFailed.collectAsState()
    val currentBootstrapStage by presenter.currentBootstrapStage.collectAsState()
    val appNameAndVersion by presenter.appNameAndVersion.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        BisqStaticScaffold(
            verticalArrangement = Arrangement.SpaceBetween,
            snackbarHostState = presenter.getSnackState()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BisqLogoGrey(modifier = Modifier.size(155.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BisqText.baseLight(
                    text = appNameAndVersion,
                    color = BisqTheme.colors.mid_grey20,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                BisqProgressBar(progress)

                BisqText.baseRegularGrey(
                    text = state,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Timeout dialog
        if (isTimeoutDialogVisible && !isBootstrapFailed) {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.timeout.title".i18n(),
                message = "mobile.bootstrap.timeout.message".i18n(currentBootstrapStage),
                confirmButtonText = "mobile.bootstrap.timeout.restart".i18n(),
                dismissButtonText = "mobile.bootstrap.timeout.continue".i18n(),
                onConfirm = { presenter.onRestartApp() },
                onDismiss = { presenter.onTimeoutDialogContinue() }
            )
        }

        // Restart dialog
        if (isBootstrapFailed) {
            WarningConfirmationDialog(
                headline = "mobile.bootstrap.failed.title".i18n(),
                message = "mobile.bootstrap.failed.message".i18n(currentBootstrapStage),
                confirmButtonText = "mobile.bootstrap.failed.restart".i18n(),
                dismissButtonText = "mobile.bootstrap.failed.shutdown".i18n(),
                onConfirm = { presenter.onRestartApp() },
                onDismiss = { presenter.onTerminateApp() }
            )
        }
    }
}
