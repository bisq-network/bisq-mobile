package network.bisq.mobile.presentation.ui.components.molecules.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ConfirmationDialog(
    title: String = "",
    message: String = "Are you sure?",
    messageColor: Color = BisqTheme.colors.white,
    messageLeftIcon: (@Composable () -> Unit)? = null,
    subMessage: String = "",
    confirmButtonText: String = "Yes",
    cancelButtonText: String = "No",
    marginTop: Dp = BisqUIConstants.ScreenPadding5X,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalButtonPlacement: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BisqDialog(
        horizontalAlignment = horizontalAlignment,
        marginTop = marginTop,
    ) {
        if (message.isNotEmpty()) {
            if (messageLeftIcon == null) {
                BisqText.h6Regular(message, color = messageColor)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    messageLeftIcon()
                    BisqGap.H1()
                    BisqText.h6Regular(message, color = messageColor)
                }
            }
            BisqGap.V1()
        }
        if (subMessage.isNotEmpty()) {
            BisqText.baseRegular(subMessage)
            BisqGap.V1()
        }
        if (verticalButtonPlacement) {
            Column {
                BisqButton(
                    text = cancelButtonText,
                    type = BisqButtonType.Grey,
                    onClick = onDismiss,
                    fullWidth = true
                )
                BisqGap.VHalf()
                BisqButton(
                    text = confirmButtonText,
                    onClick = onConfirm,
                    fullWidth = true
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                BisqButton(
                    text = cancelButtonText,
                    type = BisqButtonType.Grey,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1.0F),
                )
                BisqGap.H1()
                BisqButton(
                    text = confirmButtonText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.0F),
                )
            }
        }
    }
}