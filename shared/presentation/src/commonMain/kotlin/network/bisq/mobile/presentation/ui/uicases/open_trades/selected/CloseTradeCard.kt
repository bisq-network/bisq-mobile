package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun CloseTradeCard(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val stringsCommon = LocalStrings.current.common

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        BisqText.h4Regular(text = strings.bisqEasy_tradeState_phase4)

        BisqText.baseRegular(
            text = stringsBisqEasy.bisqEasy_openTrades_closeTrade_warning_completed,
            color = BisqTheme.colors.grey1,
            textAlign = TextAlign.Center
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BisqButton(
                text = stringsCommon.buttons_cancel,
                backgroundColor = BisqTheme.colors.dark5,
                onClick = onDismissRequest,
                padding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            )
            BisqButton(
                text = stringsBisqEasy.bisqEasy_openTrades_confirmCloseTrade,
                onClick = onConfirm,
                padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}