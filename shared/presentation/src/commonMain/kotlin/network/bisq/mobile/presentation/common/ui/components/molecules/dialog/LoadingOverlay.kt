package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Overlay that blocks screen interaction immediately while optionally showing a loading dialog.
 *
 * @param isBlocking When true, renders a transparent full-screen box that consumes all clicks
 * @param showDialog When true, renders the LoadingDialog (typically after a grace delay)
 */
@Composable
fun LoadingOverlay(
    isBlocking: Boolean,
    showDialog: Boolean,
) {
    // Transparent full-screen box that consumes all clicks
    // Rendered first to block interaction immediately
    if (isBlocking) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { /* consume all clicks - no action */ },
        )
    }

    // LoadingDialog rendered separately (not inside the Box)
    // Appears after grace delay
    if (showDialog) {
        LoadingDialog()
    }
}
