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
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofField
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.PaymentProofType
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun BuyerStateLightning3b(
    presenter: BuyerStateLightning3bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val selectedTrade by presenter.selectedTrade.collectAsState()
    val trade = selectedTrade ?: return
    val preImage by trade.bisqEasyTradeModel.paymentProof.collectAsState()

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
            // The seller has sent the Bitcoin via Lightning network
            BisqText.h5Light("bisqEasy.tradeState.info.buyer.phase3b.headline.ln".i18n())
        }
        Column {
            BisqGap.V1()
            BisqText.baseLightGrey(
                // Transfers via the Lightning Network are typically near-instant....
                "bisqEasy.tradeState.info.buyer.phase3b.info.ln".i18n(),
            )

            BisqGap.V1()
            if (preImage != null) {
                PaymentProofField(
                    // Preimage
                    label = "bisqEasy.tradeState.info.phase3b.lightning.preimage".i18n(),
                    value = preImage!!,
                    type = PaymentProofType.LightningPreImage,
                    disabled = true,
                )
            }

            BisqGap.V1()
            BisqButton(
                // Confirm receipt
                text = "bisqEasy.tradeState.info.buyer.phase3b.confirmButton.ln".i18n(),
                onClick = { presenter.onCompleteTrade() },
            )
        }
    }
}
