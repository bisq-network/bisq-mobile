package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_check_circle
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun SellerState3a(
    presenter: SellerState3aPresenter,
) {
    RememberPresenterLifecycle(presenter)

    val paymentProof by presenter.paymentProof.collectAsState()
    val buttonEnabled by presenter.buttonEnabled.collectAsState()
    val openTradeItemModel = presenter.selectedTrade.value!!
    val quoteAmount = openTradeItemModel.quoteAmountWithCode
    val baseAmount = openTradeItemModel.baseAmountWithCode
    val paymentMethod = openTradeItemModel.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
    val bitcoinPaymentDescription = "bisqEasy.tradeState.info.seller.phase3a.bitcoinPayment.description.$paymentMethod".i18n()
    val paymentProofDescription = "bisqEasy.tradeState.info.seller.phase3a.paymentProof.description.$paymentMethod".i18n()
    val paymentProofPrompt = "bisqEasy.tradeState.info.seller.phase3a.paymentProof.prompt.$paymentMethod".i18n()

    val bitcoinPaymentData = openTradeItemModel.bisqEasyTradeModel.bitcoinPaymentData.value ?: "data.na".i18n()

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painterResource(Res.drawable.trade_check_circle), "",
                modifier = Modifier.width(20.dp).height(20.dp)
            )
            BisqText.baseRegular(
                // I confirmed to have received {0}
                text = "bisqEasy.tradeState.info.seller.phase3a.fiatPaymentReceivedCheckBox".i18n(quoteAmount),
                color = BisqTheme.colors.grey2
            )
        }

        BisqGap.V1()
        // Send {0} to the buyer
        BisqText.h5Light(text = "bisqEasy.tradeState.info.seller.phase3a.sendBtc".i18n(baseAmount))

        BisqGap.VHalf()
        // todo add copy icon
        BisqTextField(
            // Amount to send
            label = "bisqEasy.tradeState.info.seller.phase3a.baseAmount".i18n(),
            value = baseAmount,
            disabled = true
        )

        BisqGap.VHalf()
        // todo add copy icon
        BisqTextField(
            // Bitcoin address / Lightning invoice
            label = bitcoinPaymentDescription,
            value = bitcoinPaymentData,
            disabled = true
        )

        BisqGap.V1()
        BisqText.baseRegular(
            // Fill in the Bitcoin transaction ID / Fill in the preimage if available
            text = paymentProofPrompt,
            color = BisqTheme.colors.grey2
        )

        BisqGap.VHalf()
        BisqTextField(
            // Transaction ID / Preimage (optional)
            label = paymentProofDescription,
            value = paymentProof ?: "",
            onValueChange = { it, isValid -> presenter.onPaymentProofInput(it) },
        )

        BisqGap.V1()
        BisqButton(
            // I confirm to have sent {0}
            text = "bisqEasy.tradeState.info.seller.phase3a.btcSentButton".i18n(baseAmount),
            onClick = { presenter.onConfirmedBtcSent() },
            disabled = buttonEnabled.not(),
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
    }
}
