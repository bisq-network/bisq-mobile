package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import network.bisq.mobile.presentation.ui.components.organisms.trades.InvalidAddressConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BuyerState1a(
    presenter: BuyerState1aPresenter
) {
    RememberPresenterLifecycle(presenter)

    val headline by presenter.headline.collectAsState()
    val description by presenter.description.collectAsState()
    val bitcoinPaymentData by presenter.bitcoinPaymentData.collectAsState()
    val addressFieldType by presenter.bitcoinLnAddressFieldType.collectAsState()
    val showInvalidAddressDialog by presenter.showInvalidAddressDialog.collectAsState()

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
        )

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.send".i18n(), // Send to seller
                onClick = { presenter.onSendClick() },
                disabled = bitcoinPaymentData.isEmpty(),
            )
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(), // Open wallet guide
                onClick = { presenter.onOpenWalletGuide() },
                type = BisqButtonType.Outline,
                padding = PaddingValues(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf
                )
            )
        }
    }

    if (showInvalidAddressDialog) {
        InvalidAddressConfirmationDialog(
            addressType = addressFieldType,
            onConfirm = presenter::onSend,
            onDismiss = { presenter.setShowInvalidAddressDialog(false) },
        )

    }
}