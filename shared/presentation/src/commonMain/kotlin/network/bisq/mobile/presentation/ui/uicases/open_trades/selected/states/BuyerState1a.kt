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
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BuyerState1a(
    presenter: BuyerState1aPresenter
) {
    RememberPresenterLifecycle(presenter)

    val headline by presenter.headline.collectAsState()
    val description by presenter.description.collectAsState()
    val bitcoinPaymentData by presenter.bitcoinPaymentData.collectAsState()

    Column {
        BisqGap.V1()
        // Fill in your Bitcoin address / Fill in your Lightning invoice
        BisqText.h5Light(text = headline)

        BisqGap.V1()
        BisqTextField(
            label = description,  // Bitcoin address / Lightning invoice
            value = bitcoinPaymentData,
            helperText = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp".i18n(), // If you have not set up a wallet yet, you can find help at the wallet guide
            onValueChange = { it, isValid -> presenter.onBitcoinPaymentDataInput(it) },
            showPaste = true,
        )

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.send".i18n(), // Send to seller
                onClick = { presenter.onSend() },
                disabled = bitcoinPaymentData.isEmpty(),
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(), // Open wallet guide
                onClick = { presenter.onOpenWalletGuide() },
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                ),
                type = BisqButtonType.Outline,
                color = BisqTheme.colors.primary,
                borderColor = BisqTheme.colors.primary,
            )
        }
    }
}