package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_fiat_payment_confirmation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BuyerState2b(
    presenter: BuyerState2bPresenter,
) {
    val openTradeItemModel = presenter.selectedTrade.value!!
    val quoteCurrencyCode = openTradeItemModel.quoteCurrencyCode
    val quoteAmountWithCode = openTradeItemModel.quoteAmountWithCode
    val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
    // Bitcoin address / Lightning invoice
    val bitcoinPaymentData = "bisqEasy.tradeState.bitcoinPaymentData.$paymentMethod".i18n()

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_fiat_payment_confirmation,
                isLoading = true
            )
            // Wait for the seller to confirm receipt of payment
            BisqText.h5Light(text = "bisqEasy.tradeState.info.buyer.phase2b.headline".i18n(quoteCurrencyCode))
        }
        Column {
            BisqGap.V2()
            BisqText.baseLightGrey(
                // Once the seller has received your payment of {0}, they will start the Bitcoin transfer to your provided {1}.
                text = "bisqEasy.tradeState.info.buyer.phase2b.info".i18n(quoteAmountWithCode, bitcoinPaymentData),
            )
        }
    }
}