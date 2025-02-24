package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CloseTradeDialog(
    onDismissCloseTrade: () -> Unit,
    onConfirmCloseTrade: () -> Unit
) {
    BisqDialog {

        BisqText.baseRegular(
            text = "bisqEasy.openTrades.closeTrade.warning.completed".i18n(),
            textAlign = TextAlign.Center
        )

        BisqGap.V2()

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = onDismissCloseTrade,
            )
            BisqButton(
                text = "bisqEasy.openTrades.confirmCloseTrade".i18n(),
                onClick = onConfirmCloseTrade,
            )
        }
    }
}