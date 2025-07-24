package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    val tradeId = openTradeItemModel.bisqEasyTradeModel.shortId

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        // Send {0} to the seller''s payment account
        BisqText.h5Light("bisqEasy.tradeState.info.buyer.phase2a.headline".i18n(quoteAmount))

        BisqGap.VHalf()
        BisqTextField(
            // Amount to transfer
            label = "bisqEasy.tradeState.info.buyer.phase2a.quoteAmount".i18n(),
            value = quoteAmount,
            disabled = true,
            showCopy = true,
        )

        BisqGap.VHalf()
        BisqTextField(
            // Payment account of seller
            label = "bisqEasy.tradeState.info.buyer.phase2a.sellersAccount".i18n(),
            // In Bisq Easy we show the Reason for payment with the trade ID as extra field, but on mobile we don't want to
            // use up too much space for that and show it as helper text instead.
            // Use the trade ID {0} for the 'Reason for payment' field
            helperText = "mobile.tradeState.info.buyer.phase2a.reasonForPaymentInfo".i18n(tradeId),
            value = paymentAccountData,
            disabled = true,
            showCopy = true,
        )

        BisqGap.V1()
        BisqButton(
            // Confirm payment of {0}
            text = "bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent".i18n(quoteAmount),
            onClick = { presenter.onConfirmFiatSent() },
        )
    }
}
