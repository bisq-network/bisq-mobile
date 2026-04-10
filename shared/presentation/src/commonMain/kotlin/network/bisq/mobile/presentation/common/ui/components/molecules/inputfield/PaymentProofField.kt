package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.PasteIconButton
import network.bisq.mobile.presentation.common.ui.utils.BitcoinTransactionValidation
import network.bisq.mobile.presentation.common.ui.utils.LightningPreImageValidation

enum class PaymentProofType {
    BitcoinTx,
    LightningPreImage,
}

@Composable
fun PaymentProofField(
    value: String,
    modifier: Modifier = Modifier,
    label: String = "",
    onValueChange: (String, Boolean) -> Unit = { _, _ -> },
    disabled: Boolean = false,
    type: PaymentProofType = PaymentProofType.BitcoinTx,
    direction: DirectionEnum = DirectionEnum.BUY,
) {
    val validation: (String) -> String? =
        remember(type) {
            {
                when (type) {
                    PaymentProofType.BitcoinTx -> {
                        if (BitcoinTransactionValidation.validateTxId(it)) {
                            null
                        } else {
                            "validation.invalidBitcoinTransactionId".i18n()
                        }
                    }

                    PaymentProofType.LightningPreImage -> {
                        if (LightningPreImageValidation.validatePreImage(it)) {
                            null
                        } else {
                            "validation.invalidLightningPreimage".i18n()
                        }
                    }
                }
            }
        }

    var errorMessage by remember(type, value) {
        mutableStateOf(if (value.isNotBlank()) validation(value) else null)
    }

    BisqTextFieldV0(
        label = label,
        value = value,
        onValueChange = { newValue ->
            val newErrorMessage = validation(newValue)
            errorMessage = newErrorMessage
            onValueChange(newValue, newErrorMessage == null)
        },
        enabled = !disabled,
        modifier = modifier,
        readOnly = disabled,
        trailingIcon =
            when (direction) {
                DirectionEnum.BUY -> ({ CopyIconButton(value = value) })
                DirectionEnum.SELL -> (
                    if (disabled) {
                        null
                    } else {
                        {
                            PasteIconButton(onPaste = { pastedValue ->
                                val pastedErrorMessage = validation(pastedValue)
                                errorMessage = pastedErrorMessage
                                onValueChange(pastedValue, pastedErrorMessage == null)
                            })
                        }
                    }
                )
            },
        isError = errorMessage != null,
        bottomMessage = errorMessage,
    )
}
