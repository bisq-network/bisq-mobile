package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.InfoIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
        Row(
            horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            modifier = Modifier.height(IntrinsicSize.Max)
        ) {
            BisqButton(
                modifier = Modifier.fillMaxHeight(),
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = onDismiss,
                padding = PaddingValues(horizontal = 42.dp, vertical = 8.dp)
            )
            BisqButton(
                modifier = Modifier.fillMaxHeight(),
                text = "bisqEasy.mediation.request.confirm.openMediation".i18n(),
                backgroundColor = BisqTheme.colors.primary,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            )
        }
    }
}