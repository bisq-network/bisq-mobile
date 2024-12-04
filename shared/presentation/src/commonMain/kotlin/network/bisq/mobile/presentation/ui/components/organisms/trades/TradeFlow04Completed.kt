package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TradeFlow04Completed(
    onNext: () -> Unit
){
    Column {
        BisqText.h6Regular(
            text = "Trade was successfully completed"
        )
        BisqTextField(
            value = "10000.02 USD",
            onValueChanged = {},
            label = "You have received"
        )
        BisqTextField(
            value = "0.00173399 BTC",
            onValueChanged = {},
            label = "You have sold"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BisqButton(
                text = "Close trade",
                color = BisqTheme.colors.primary,
                onClick = onNext,
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
                text = "Explore trade data",
                color = BisqTheme.colors.light1,
                onClick = {},
                backgroundColor = BisqTheme.colors.dark5,
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
        }
    }
}