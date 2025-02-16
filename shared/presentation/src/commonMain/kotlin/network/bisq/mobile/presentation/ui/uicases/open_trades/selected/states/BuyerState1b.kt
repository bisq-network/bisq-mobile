package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_account_data
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BuyerState1b(
) {
    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_account_data,
                isLoading = true
            )
            // Wait for the seller's payment account data
            BisqText.h5Light(text = "bisqEasy.tradeState.info.buyer.phase1b.headline".i18n())
        }
        Column {
            BisqGap.V2()
            BisqText.baseLightGrey(
                // You can use the chat below for getting in touch with the seller.
                text = "bisqEasy.tradeState.info.buyer.phase1b.info".i18n(),
            )
        }
    }
}