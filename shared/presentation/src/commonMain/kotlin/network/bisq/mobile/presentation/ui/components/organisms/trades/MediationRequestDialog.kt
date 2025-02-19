package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.InfoIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun MediationRequestDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BisqDialog {
        Row {
            InfoIcon()
            BisqGap.H1()
            BisqText.h6Regular(
                text = "Request mediation",
                color = BisqTheme.colors.primary
            )
        }
        BisqGap.V1()
        Column {
            BisqText.baseRegular(
                text = "If you have problems which you cannot resolve with your trade partner your can request assistance from a mediator.",
                textAlign = TextAlign.Center
            )
            BisqGap.V1()
            BisqText.smallRegularGrey(
                text = "Please do not request mediation for general questions. In the support section there are chat rooms where you can get general advice and help.",
                textAlign = TextAlign.Center
            )
        }
        BisqGap.V1()
        Row {
            BisqButton(
                text = "Cancel",
                type = BisqButtonType.Grey,
                onClick = onDismiss,
                padding = PaddingValues(horizontal = 42.dp, vertical = 8.dp)
            )
            BisqGap.H1()
            BisqButton(
                text = "Open mediation",
                backgroundColor = BisqTheme.colors.primary,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}