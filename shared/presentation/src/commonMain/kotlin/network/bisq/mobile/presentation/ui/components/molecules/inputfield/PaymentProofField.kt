package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.helpers.BitcoinTransactionValidation
import network.bisq.mobile.presentation.ui.helpers.LightningPreImageValidation


enum class PaymentProofType {
    BitcoinTx,
    LightningPreImage,
}
@Composable
fun PaymentProofField(
    label: String = "",
    value: String,
    onValueChange: ((String, Boolean) -> Unit)? = null,
    disabled: Boolean = false,
    type: PaymentProofType = PaymentProofType.BitcoinTx,
    modifier: Modifier = Modifier
) {
    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        disabled = disabled,
        showPaste = true,
        modifier = modifier,
        validation = {
            if (type == PaymentProofType.BitcoinTx) {
                if (BitcoinTransactionValidation.validateTxId(it)) {
                    return@BisqTextField null
                }
                return@BisqTextField "validation.invalidBitcoinTransactionId".i18n()
            } else if (type == PaymentProofType.LightningPreImage) {
                if (LightningPreImageValidation.validatePreImage(it)) {
                    return@BisqTextField null
                }
                return@BisqTextField "validation.invalidLightningPreimage".i18n()
            }

            return@BisqTextField null
        }
    )

}
