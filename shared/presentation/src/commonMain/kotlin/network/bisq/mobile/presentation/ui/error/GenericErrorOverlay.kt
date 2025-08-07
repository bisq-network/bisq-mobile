import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.organisms.ReportBugPanel
import network.bisq.mobile.presentation.ui.components.organisms.TrustedNodeAPIIncompatiblePopup
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import org.koin.compose.koinInject

@Composable
fun ErrorOverlay() {
    val appPresenter: AppPresenter = koinInject()

    val errorMessage by GenericErrorHandler.genericErrorMessage.collectAsState()
    val isUncaughtException by GenericErrorHandler.isUncaughtException.collectAsState()

    errorMessage?.let {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Dim the background
        ) {
            // TODO: Should define exception types.
            // For this specific issue, have a type like TRUST_NODE_VERSION_INCOMPATIBLE
            if (it.startsWith("Your configured trusted ")) {
                TrustedNodeAPIIncompatiblePopup(
                    errorMessage = it,
                    onFix = {
                        appPresenter.navigateToTrustedNode()
                        GenericErrorHandler.clearGenericError()
                    }
                )
            } else {
                ReportBugPanel(
                    errorMessage = it,
                    isUncaughtException = isUncaughtException,
                    onClose = { GenericErrorHandler.clearGenericError() }
                )
            }
        }
    }
}