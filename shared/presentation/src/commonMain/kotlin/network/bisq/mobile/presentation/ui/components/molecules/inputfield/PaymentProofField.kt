package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
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
    modifier: Modifier = Modifier,
    direction: DirectionEnum = DirectionEnum.BUY,
) {
    val validation: (String) -> String? = remember(type) {
        {
            when (type) {
                PaymentProofType.BitcoinTx -> {
                    if (BitcoinTransactionValidation.validateTxId(it)) null
                    else "validation.invalidBitcoinTransactionId".i18n()
                }

                PaymentProofType.LightningPreImage -> {
                    if (LightningPreImageValidation.validatePreImage(it)) null
                    else "validation.invalidLightningPreimage".i18n()
                }
            }
        }
    }

    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        disabled = disabled,
        showCopy = direction == DirectionEnum.BUY,
        showPaste = direction == DirectionEnum.SELL,
        modifier = modifier,
        validation = validation,
    )

}
