package network.bisq.mobile.presentation.common.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun InvalidAddressConfirmationDialog(
    addressType: BitcoinLnAddressFieldType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val warningText =
        if (addressType == BitcoinLnAddressFieldType.Bitcoin) {
            "bisqEasy.takeOffer.bitcoinPaymentData.warning.MAIN_CHAIN".i18n()
        } else {
            "bisqEasy.takeOffer.bitcoinPaymentData.warning.LN".i18n()
        }

    WarningConfirmationDialog(
        message = warningText,
        dismissButtonText = "action.close".i18n(),
        confirmButtonText = "bisqEasy.takeOffer.bitcoinPaymentData.warning.proceed".i18n(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        verticalButtonPlacement = true,
    )
}

// Pattern-mismatch escape hatch only: data over the protocol max length never reaches this
// dialog — BuyerState1aPresenter hard-blocks it with an error snackbar instead (#1795).
@Preview
@Composable
private fun InvalidAddressConfirmationDialog_BitcoinPreview() {
    BisqTheme.Preview {
        InvalidAddressConfirmationDialog(
            addressType = BitcoinLnAddressFieldType.Bitcoin,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun InvalidAddressConfirmationDialog_LightningPreview() {
    BisqTheme.Preview {
        InvalidAddressConfirmationDialog(
            addressType = BitcoinLnAddressFieldType.Lightning,
            onDismiss = {},
            onConfirm = {},
        )
    }
}
