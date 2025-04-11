import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

    val errorMessage = GenericErrorHandler.genericErrorMessage.collectAsState().value
    val isUncaughtException = GenericErrorHandler.isUncaughtException.collectAsState().value

    if (errorMessage != null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Dim the background
        ) {
            // TODO: Should define exception types.
            // For this specific issue, have a type like TRUST_NODE_VERSION_INCOMPATIBLE
            if (errorMessage.startsWith("Your configured trusted ")) {
                TrustedNodeAPIIncompatiblePopup(
                    errorMessage = errorMessage,
                    onFix = {
                        appPresenter.navigateToTrustedNode()
                        GenericErrorHandler.clearGenericError()
                    }
                )
            } else {
                ReportBugPanel(
                    errorMessage = errorMessage,
                    isUncaughtException = isUncaughtException,
                    onClose = { GenericErrorHandler.clearGenericError() }
                )
            }
        }
    }
}