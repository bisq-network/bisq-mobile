package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.helpers.BitcoinAddressValidation
import network.bisq.mobile.presentation.ui.helpers.LightningInvoiceValidation

enum class BitcoinLnAddressFieldType {
    Bitcoin,
    Lightning,
}

@Composable
fun BitcoinLnAddressField(
    label: String = "",
    value: String,
    onValueChange: (String, Boolean) -> Unit,
    disabled: Boolean = false,
    type: BitcoinLnAddressFieldType = BitcoinLnAddressFieldType.Bitcoin,
    modifier: Modifier = Modifier
) {
    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        disabled = disabled,
        showPaste = true,
        modifier = modifier,
        helperText = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp".i18n(), // If you have not set up a wallet yet, you can find help at the wallet guide
        validation = {
            if (type == BitcoinLnAddressFieldType.Bitcoin) {
                if (BitcoinAddressValidation.validateAddress(it)) {
                    return@BisqTextField null
                }
                return@BisqTextField "validation.invalidBitcoinAddress".i18n()
            } else {
                if (LightningInvoiceValidation.validateInvoice(it)) {
                    return@BisqTextField null
                }
                return@BisqTextField "validation.invalidLightningInvoice".i18n()
            }
        }
    )

}
