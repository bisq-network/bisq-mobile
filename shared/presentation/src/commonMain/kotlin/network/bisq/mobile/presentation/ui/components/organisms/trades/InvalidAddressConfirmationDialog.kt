package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun InvalidAddressConfirmationDialog(
    addressType: BitcoinLnAddressFieldType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val warningText = if (addressType == BitcoinLnAddressFieldType.Bitcoin) {
        "bisqEasy.takeOffer.bitcoinPaymentData.warning.MAIN_CHAIN".i18n()
    } else {
        "bisqEasy.takeOffer.bitcoinPaymentData.warning.LN".i18n()
    }

    WarningConfirmationDialog(
        subMessage = warningText,
        cancelButtonText = "action.close".i18n(),
        confirmButtonText = "bisqEasy.takeOffer.bitcoinPaymentData.warning.proceed".i18n(),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        verticalButtonPlacement = true,
    )

}