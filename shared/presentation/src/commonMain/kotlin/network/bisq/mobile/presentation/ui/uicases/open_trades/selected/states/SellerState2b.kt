package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun SellerState2b(
    presenter: SellerState2bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val shortId = presenter.selectedTrade.value!!.shortTradeId
    val quoteAmountWithCode = presenter.selectedTrade.value!!.quoteAmountWithCode

    Column {
        BisqGap.V1()
        BisqGap.VHalf()
        // Check if you have received {0} with reason for payment ''{1}''
        BisqText.h5Light("bisqEasy.tradeState.info.seller.phase2b.headline".i18n(quoteAmountWithCode, shortId))

        BisqGap.V1()
        BisqText.baseLightGrey(
            // Visit your bank account or payment provider app to confirm receipt of the buyer's payment.
            "bisqEasy.tradeState.info.seller.phase2b.info".i18n(quoteAmountWithCode),
        )

        BisqGap.V1()
        BisqButton(
            // Confirm receipt of {0}
            text = "bisqEasy.tradeState.info.seller.phase2b.fiatReceivedButton".i18n(quoteAmountWithCode),
            onClick = { presenter.onConfirmFiatReceipt() },
        )
    }
}