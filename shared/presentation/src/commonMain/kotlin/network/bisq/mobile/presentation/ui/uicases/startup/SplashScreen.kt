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
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoCircle
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
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

    Box(modifier = Modifier.fillMaxSize()) {
        BisqStaticScaffold(
            verticalArrangement = Arrangement.SpaceBetween,
            snackbarHostState = presenter.getSnackState()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BisqLogoCircle(modifier = Modifier.size(140.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                headline = "bootstrap.timeout.title".i18n(),
                message = "bootstrap.timeout.message".i18n(currentBootstrapStage),
                confirmButtonText = "bootstrap.timeout.restart".i18n(),
                dismissButtonText = "bootstrap.timeout.continue".i18n(),
                onConfirm = { presenter.onRestart() },
                onDismiss = { presenter.onTimeoutDialogContinue() }
            )
        }

        // Restart dialog
        if (isBootstrapFailed) {
            WarningConfirmationDialog(
                headline = "bootstrap.failed.title".i18n(),
                message = "bootstrap.failed.message".i18n(currentBootstrapStage),
                confirmButtonText = "bootstrap.failed.restart".i18n(),
                dismissButtonText = "bootstrap.failed.shutdown".i18n(),
                onConfirm = { presenter.onRestart() },
                onDismiss = { presenter.onTerminateApp() }
            )
        }
    }
}
