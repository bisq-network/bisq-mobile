package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun BuyerState2a(
    presenter: BuyerState2aPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val openTradeItemModel = presenter.selectedTrade.value!!
    val quoteAmount = openTradeItemModel.quoteAmountWithCode
    val paymentAccountData = openTradeItemModel.bisqEasyTradeModel.paymentAccountData.value ?: "data.na".i18n()

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        // Send {0} to the seller''s payment account
        BisqText.h5Light(text = "bisqEasy.tradeState.info.buyer.phase2a.headline".i18n(quoteAmount))

        BisqGap.VHalf()
        // todo add copy icon
        BisqTextField(
            // Amount to transfer
            label = "bisqEasy.tradeState.info.buyer.phase2a.quoteAmount".i18n(),
            value = quoteAmount,
            disabled = true,
            showCopy = true,
        )

        BisqGap.VHalf()
        // todo add copy icon
        BisqTextField(
            // Payment account of seller
            label = "bisqEasy.tradeState.info.buyer.phase2a.sellersAccount".i18n(),
            // Please leave the 'Reason for payment' field empty, in case you make a bank transfer
            helperText = "bisqEasy.tradeState.info.buyer.phase2a.reasonForPaymentInfo".i18n(),
            value = paymentAccountData,
            disabled = true,
            showCopy = true,
        )

        BisqGap.V1()
        BisqButton(
            // Confirm payment of {0}
            text = "bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n(quoteAmount),
            onClick = { presenter.onConfirmFiatSent() },
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
    }
}
