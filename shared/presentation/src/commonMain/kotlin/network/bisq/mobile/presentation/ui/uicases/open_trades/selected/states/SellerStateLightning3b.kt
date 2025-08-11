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
import bisqapps.shared.presentation.generated.resources.trade_bitcoin_confirmation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.icons.CheckCircleIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun SellerStateLightning3b(
    presenter: SellerStateLightning3bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val buyerHasConfirmedBitcoinReceipt by presenter.buyerHasConfirmedBitcoinReceipt.collectAsState()

    if (buyerHasConfirmedBitcoinReceipt) {
        SellerStateLightning3bPaymentConfirmed(presenter)
    } else {
        SellerStateLightning3bWaitingForPayment(presenter)
    }

}

@Composable
private fun SellerStateLightning3bWaitingForPayment(
    presenter: SellerStateLightning3bPresenter,
) {
    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_bitcoin_confirmation,
                isLoading = true
            )
            // Wait for buyer confirming Bitcoin receipt
            BisqText.h5Light("bisqEasy.tradeState.info.seller.phase3b.headline.ln".i18n())
        }

        Column {
            BisqGap.V1()
            BisqText.baseLightGrey(
                // Transfers via the Lightning Network are usually near-instant and reliable....
                "bisqEasy.tradeState.info.seller.phase3b.info.ln".i18n(),
            )

            BisqGap.V1()
            BisqButton(
                // Skip waiting for buyer's confirmation
                text = "bisqEasy.tradeState.info.seller.phase3b.confirmButton.skipWaitForConfirmation.ln".i18n(),
                type = BisqButtonType.Grey,
                onClick = presenter::skipWaiting
            )
        }
    }
}

@Composable
private fun SellerStateLightning3bPaymentConfirmed(
    presenter: SellerStateLightning3bPresenter,
) {
    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
        ) {
            CheckCircleIcon()

            // The buyer has confirmed the Bitcoin receipt
            BisqText.baseLightGrey("bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.ln".i18n())
        }

        BisqGap.V1()
        BisqText.h5Light(
            // Your Bitcoin transfer has been confirmed
            "bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.headline.ln".i18n(),
        )
        BisqGap.VHalf()
        // You can now move forward to finalize your trade and view a complete summary of the transaction.
        BisqText.baseLightGrey("bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.info.ln".i18n())

        BisqGap.V1()
        BisqButton(
            // Complete trade
            text = "bisqEasy.tradeState.info.seller.phase3b.confirmButton.ln".i18n(),
            onClick = presenter::completeTrade,
        )
    }
}