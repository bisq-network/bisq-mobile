import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.components.organisms.GenericErrorPanel
import network.bisq.mobile.presentation.ui.components.organisms.ReportBugPanel

@Composable
fun ErrorOverlay(errorMessage: String?, systemCrashed: Boolean, onClose: () -> Unit) {

    if (errorMessage != null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Dim the background
        ) {
            ReportBugPanel(
                errorMessage = errorMessage,
                systemCrashed = systemCrashed,
                onClose = { onClose() }
            )
        }
    }
}