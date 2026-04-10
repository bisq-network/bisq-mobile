package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.PasteIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ScanQrIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.BitcoinAddressValidation
import network.bisq.mobile.presentation.common.ui.utils.LightningInvoiceValidation
import network.bisq.mobile.presentation.common.ui.utils.PreviewEnvironment

enum class BitcoinLnAddressFieldType {
    Bitcoin,
    Lightning,
}

@Composable
fun BitcoinLnAddressField(
    value: String,
    label: String = "",
    onValueChange: (String, Boolean) -> Unit = { _, _ -> },
    disabled: Boolean = false,
    type: BitcoinLnAddressFieldType = BitcoinLnAddressFieldType.Bitcoin,
    onBarcodeClick: (() -> Unit)? = null,
    triggerValidation: Int? = null,
) {
    val validationLogic =
        remember(type) {
            when (type) {
                BitcoinLnAddressFieldType.Bitcoin -> { input: String ->
                    if (BitcoinAddressValidation.validateAddress(input)) {
                        null
                    } else {
                        "validation.invalidBitcoinAddress".i18n()
                    }
                }

                BitcoinLnAddressFieldType.Lightning -> { input: String ->
                    if (LightningInvoiceValidation.validateInvoice(input)) {
                        null
                    } else {
                        "validation.invalidLightningInvoice".i18n()
                    }
                }
            }
        }

    var errorMessage by remember(type, value) {
        mutableStateOf(
            if (value.isNotBlank()) {
                validationLogic(value)
            } else {
                null
            },
        )
    }

    val helperText = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp".i18n()

    LaunchedEffect(triggerValidation) {
        if (triggerValidation != null && value.isNotBlank()) {
            val newErrorMessage = validationLogic(value)
            errorMessage = newErrorMessage
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqTextFieldV0(
            label = label,
            value = value,
            onValueChange = { newValue ->
                val newErrorMessage = if (newValue.isBlank()) null else validationLogic(newValue)
                errorMessage = newErrorMessage
                onValueChange(newValue, newErrorMessage == null)
            },
            enabled = !disabled,
            readOnly = disabled,
            modifier = Modifier.weight(1f),
            trailingIcon =
                if (!disabled) {
                    {
                        PasteIconButton(
                            onPaste = { pastedValue ->
                                errorMessage = validationLogic(pastedValue)
                                onValueChange(pastedValue, errorMessage == null)
                            },
                        )
                    }
                } else {
                    null
                },
            isError = errorMessage != null,
            bottomMessage = errorMessage ?: helperText,
        )
        if (!disabled) {
            Column {
                // a little hack to align the button with input
                if (label.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.BaseLight(
                            text = " ",
                            color = Color.Transparent,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
                        )
                    }
                    BisqGap.VQuarter()
                }
                // end of the hack
                BisqButton(
                    backgroundColor = BisqTheme.colors.secondary,
                    onClick = onBarcodeClick,
                    modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
                    iconOnly = {
                        ScanQrIcon()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun BitcoinLnAddressFieldPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "",
                onBarcodeClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun BitcoinLnAddressFieldWithLabelPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "",
                label = "Test",
                onBarcodeClick = { },
            )
        }
    }
}

@Preview
@Composable
private fun BitcoinLnAddressFieldInvalidPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "Test",
                onBarcodeClick = { },
            )
        }
    }
}
