package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_fiat_payment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun SellerState2a(
    presenter: SellerState2aPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val quoteCurrencyCode = presenter.selectedTrade.value!!.quoteCurrencyCode
    val quoteAmountWithCode = presenter.selectedTrade.value!!.quoteAmountWithCode

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_fiat_payment,
                isLoading = true
            )
            BisqText.h5Light("bisqEasy.tradeState.info.seller.phase2a.waitForPayment.headline".i18n(quoteCurrencyCode))
        }
        Column {
            BisqGap.V2()
            BisqText.baseLightGrey(
                // Once the buyer has initiated the payment of {0}, you will get notified.
                "bisqEasy.tradeState.info.seller.phase2a.waitForPayment.info".i18n(quoteAmountWithCode),
            )
        }
    }
}