package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.InfoIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun MediationRequestDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BisqDialog(
        onDismissRequest = onDismiss,
    ) {
        Row {
            InfoIcon()
            BisqGap.H1()
            BisqText.h6Regular(
                text = "bisqEasy.tradeState.requestMediation".i18n(),
                color = BisqTheme.colors.primary
            )
        }
        BisqGap.V1()
        BisqText.baseRegular(
            text = "bisqEasy.mediation.request.confirm.msg".i18n(),
            textAlign = TextAlign.Center
        )
        BisqGap.V1()
        Row {
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = onDismiss,
                padding = PaddingValues(horizontal = 42.dp, vertical = 8.dp)
            )
            BisqGap.H1()
            BisqButton(
                text = "bisqEasy.mediation.request.confirm.openMediation".i18n(),
                backgroundColor = BisqTheme.colors.primary,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}