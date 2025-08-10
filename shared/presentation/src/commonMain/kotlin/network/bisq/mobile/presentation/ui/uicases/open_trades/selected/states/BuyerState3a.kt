package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_bitcoin_payment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap

@Composable
fun BuyerState3a(
    presenter: BuyerState3aPresenter,
) {
    val selectedTrade by presenter.selectedTrade.collectAsState()

    val trade = selectedTrade ?: return

    val quoteCurrencyCode = trade.quoteCurrencyCode
    val paymentMethod = trade.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
    // Bitcoin address / Lightning invoice
    val bitcoinPaymentData = "bisqEasy.tradeState.bitcoinPaymentData.$paymentMethod".i18n()

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_bitcoin_payment,
                isLoading = true
            )
            // Wait for the seller's Bitcoin settlement
            BisqText.h5Light("bisqEasy.tradeState.info.buyer.phase3a.headline".i18n(quoteCurrencyCode))
        }
        Column {
            BisqGap.V2()
            BisqText.baseLightGrey(
                // Once the seller has received your payment of {0}, they will start the Bitcoin transfer to your provided {1}.
                "bisqEasy.tradeState.info.buyer.phase3a.info".i18n(bitcoinPaymentData),
            )
        }
    }
}
