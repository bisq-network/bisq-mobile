package network.bisq.mobile.presentation.common.ui.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.organisms.ReportBugPanel
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun GenericErrorOverlay() {
    val errorMessage by GenericErrorHandler.genericErrorMessage.collectAsState()
    val isUncaughtException by GenericErrorHandler.isUncaughtException.collectAsState()

    errorMessage?.let {
        Box(
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.5f)) // Dim the background
        ) {
            ReportBugPanel(
                errorMessage = it,
                isUncaughtException = isUncaughtException,
                onClose = { GenericErrorHandler.clearGenericError() }
            )
        }
    }
}