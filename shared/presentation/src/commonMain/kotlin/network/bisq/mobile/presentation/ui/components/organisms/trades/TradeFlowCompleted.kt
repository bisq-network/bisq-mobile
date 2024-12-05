package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

/**
 * Trade flow's 4th Stepper section
 */
@Composable
fun TradeFlowCompleted(
    onClose: () -> Unit,
    onExport: () -> Unit,
){
    val strings = LocalStrings.current.bisqEasyTradeState

    val sendAmount = "1234.56 USD"
    val btcValue = "0.00173399 BTC"

    Column {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        BisqText.h6Regular(
            text = strings.bisqEasy_tradeCompleted_title
        )
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        BisqTextField(
            value = sendAmount,
            onValueChanged = {},
            label = strings.bisqEasy_tradeCompleted_body_you_have_receveid,
        )
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        BisqTextField(
            value = btcValue,
            onValueChanged = {},
            label = strings.bisqEasy_tradeCompleted_body_you_have_sold
        )
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BisqButton(
                text = strings.bisqEasy_tradeState_info_phase4_leaveChannel,
                color = BisqTheme.colors.primary,
                onClick = onClose,
                backgroundColor = BisqTheme.colors.dark5,
                border = BorderStroke(
                    width = 2.dp,
                    color = BisqTheme.colors.primary
                ),
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
            BisqButton(
                text = strings.bisqEasy_tradeState_info_phase4_exportTrade,
                color = BisqTheme.colors.light1,
                onClick = onExport,
                backgroundColor = BisqTheme.colors.dark5,
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
        }
    }
}