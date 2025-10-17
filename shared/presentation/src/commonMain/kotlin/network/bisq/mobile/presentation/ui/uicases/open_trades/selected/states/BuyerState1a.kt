package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BitcoinLnAddressField
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BuyerState1a(
    presenter: BuyerState1aPresenter
) {
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val headline by presenter.headline.collectAsState()
    val description by presenter.description.collectAsState()
    val bitcoinPaymentData by presenter.bitcoinPaymentData.collectAsState()
    val addressFieldType by presenter.bitcoinLnAddressFieldType.collectAsState()
    val triggerBitcoinLnAddressValidation by presenter.triggerBitcoinLnAddressValidation.collectAsState()

    Column {
        BisqGap.V1()
        // Fill in your Bitcoin address / Fill in your Lightning invoice
        BisqText.h5Light(headline)

        BisqGap.V1()
        BitcoinLnAddressField(
            label = description,  // Bitcoin address / Lightning invoice
            value = bitcoinPaymentData,
            onValueChange = { it, isValid ->
                presenter.onBitcoinPaymentDataInput(it, isValid)
            },
            type = addressFieldType,
            onBarcodeClick = presenter::onBarcodeClick,
            triggerValidation = triggerBitcoinLnAddressValidation
        )

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
        ) {
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.send".i18n(), // Send to seller
                onClick = { presenter.onSendClick() },
                disabled = bitcoinPaymentData.isEmpty(),
                modifier = Modifier.fillMaxHeight(),
            )
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(), // Open wallet guide
                disabled = !isInteractive,
                onClick = { presenter.onOpenWalletGuide() },
                type = BisqButtonType.Outline,
                padding = PaddingValues(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf
                ),
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }

}