package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SellerState2b(
    presenter: SellerState2bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val quoteAmountWithCode = presenter.selectedTrade.value!!.quoteAmountWithCode

    Column {
        BisqGap.V1()
        BisqGap.VHalf()
        // Check if you have received {0}
        BisqText.h5Light(text = "bisqEasy.tradeState.info.seller.phase2b.headline".i18n(quoteAmountWithCode))

        BisqGap.V1()
        BisqText.baseLight(
            // Visit your bank account or payment provider app to confirm receipt of the buyer's payment.
            text = "bisqEasy.tradeState.info.seller.phase2b.info".i18n(quoteAmountWithCode),
            color = BisqTheme.colors.grey2
        )

        BisqGap.V1()
        BisqButton(
            // Confirm receipt of {0}
            text = "bisqEasy.tradeState.info.seller.phase2b.fiatReceivedButton".i18n(quoteAmountWithCode),
            onClick = { presenter.onConfirmFiatReceipt() },
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
    }
}