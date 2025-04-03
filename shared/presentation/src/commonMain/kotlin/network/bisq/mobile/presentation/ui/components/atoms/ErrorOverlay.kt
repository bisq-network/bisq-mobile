import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.organisms.GenericErrorPanel
import network.bisq.mobile.presentation.ui.components.organisms.ReportBugPanel
import network.bisq.mobile.presentation.ui.components.organisms.TrustedNodeAPIIncompatiblePopup
import org.koin.compose.koinInject

@Composable
fun ErrorOverlay(
    errorMessage: String?,
    systemCrashed: Boolean,
    onClose: () -> Unit
) {
    val presenter: AppPresenter = koinInject()

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
                        presenter.navigateToTrustedNode()
                        MainPresenter._genericErrorMessage.value = null
                    }
                )
            } else {
                ReportBugPanel(
                    errorMessage = errorMessage,
                    systemCrashed = systemCrashed,
                    onClose = { onClose() }
                )
            }
        }
    }
}