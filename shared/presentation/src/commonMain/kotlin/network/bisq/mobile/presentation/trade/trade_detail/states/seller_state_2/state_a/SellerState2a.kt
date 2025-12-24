@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_2.state_a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_fiat_payment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle

@Composable
fun SellerState2a(
    presenter: SellerState2aPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val selectedTrade by presenter.selectedTrade.collectAsState()
    val trade = selectedTrade ?: return

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_fiat_payment,
                isLoading = true,
            )
            BisqText.H5Light("bisqEasy.tradeState.info.seller.phase2a.waitForPayment.headline".i18n(trade.quoteCurrencyCode))
        }
        Column {
            BisqGap.V2()
            BisqText.BaseLightGrey(
                // Once the buyer has initiated the payment of {0}, you will get notified.
                "bisqEasy.tradeState.info.seller.phase2a.waitForPayment.info".i18n(trade.quoteAmountWithCode),
            )
        }
    }
}
