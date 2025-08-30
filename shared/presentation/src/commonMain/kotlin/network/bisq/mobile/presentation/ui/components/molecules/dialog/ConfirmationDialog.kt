package network.bisq.mobile.presentation.ui.components.molecules.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ConfirmationDialog(
    headline: String = "confirmation.areYouSure".i18n(),
    headlineColor: Color = BisqTheme.colors.white,
    headlineLeftIcon: (@Composable () -> Unit)? = null,
    message: String = "",
    confirmButtonText: String = "confirmation.yes".i18n(),
    dismissButtonText: String = "confirmation.no".i18n(),
    marginTop: Dp = BisqUIConstants.ScreenPadding8X,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalButtonPlacement: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BisqDialog(
        horizontalAlignment = horizontalAlignment,
        marginTop = marginTop,
        onDismissRequest = onDismiss
    ) {
        if (headline.isNotEmpty()) {
            if (headlineLeftIcon == null) {
                BisqText.h6Regular(headline, color = headlineColor)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    headlineLeftIcon()
                    BisqGap.H1()
                    BisqText.h6Regular(headline, color = headlineColor)
                }
            }
            BisqGap.V2()
        }
        if (message.isNotEmpty()) {
            BisqText.baseLight(message)
            BisqGap.V2()
        }
        if (verticalButtonPlacement) {
            Column {
                if (confirmButtonText.isNotBlank()) {
                    BisqButton(
                        text = confirmButtonText,
                        onClick = onConfirm,
                        fullWidth = true
                    )
                }
                if (confirmButtonText.isNotBlank() && dismissButtonText.isNotBlank()) {
                    BisqGap.VHalf()
                }
                if (dismissButtonText.isNotBlank()) {
                    BisqButton(
                        text = dismissButtonText,
                        type = BisqButtonType.Grey,
                        onClick = onDismiss,
                        fullWidth = true
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                if (confirmButtonText.isNotBlank()) {
                    BisqButton(
                        modifier = Modifier.weight(1.0F).fillMaxHeight(),
                        text = confirmButtonText,
                        onClick = onConfirm,
                    )
                }
                if (dismissButtonText.isNotBlank()) {
                    BisqButton(
                        modifier = Modifier.weight(1.0F).fillMaxHeight(),
                        text = dismissButtonText,
                        type = BisqButtonType.Grey,
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}