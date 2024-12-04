package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TradeFlow01AccountDetails(
    onNext: () -> Unit
) {
    Column {
        BisqText.smallRegular(
            text = "Waiting for the seller to provide their account information. Meanwhile you can provide your settlement details."
        )
        BisqText.h6Regular(
            text = "Fill in your Lightning invoice"
        )
        BisqTextField(
            placeholder = "",
            value = "lncb21h345t34io",
            onValueChanged = {},
            label = "Lightning Invoice"
        )
        BisqButton(
            text = "Send to seller",
            onClick = onNext,
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BisqText.xsmallMedium(
                text = "If you don’t have a wallet yet, refer to our"
            )
            BisqText.xsmallMedium(
                text = "Wallet guide",
                modifier = Modifier.clip(shape = RoundedCornerShape(4.dp))
                    .background(color = BisqTheme.colors.primary)
                    .padding(vertical = 2.dp, horizontal = 8.dp)
            )
        }
    }
}