package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CloseTradeDialog(
    onDismissCloseTrade: () -> Unit,
    onConfirmCloseTrade: () -> Unit
) {
    BisqDialog {
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            BisqText.baseRegular(
                text = "bisqEasy.openTrades.closeTrade.warning.completed".i18n(),
                color = BisqTheme.colors.grey1,
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                BisqButton(
                    text = "action.cancel".i18n(),
                    backgroundColor = BisqTheme.colors.dark5,
                    onClick = onDismissCloseTrade,
                    padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf)
                )
                BisqButton(
                    text = "bisqEasy.openTrades.confirmCloseTrade".i18n(),
                    onClick = onConfirmCloseTrade,
                    padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf)
                )
            }
        }
    }
}