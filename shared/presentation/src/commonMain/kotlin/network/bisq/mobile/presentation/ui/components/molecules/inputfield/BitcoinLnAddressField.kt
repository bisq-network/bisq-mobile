package network.bisq.mobile.presentation.ui.components.molecules.inputfield

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
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ScanQrIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.BitcoinAddressValidation
import network.bisq.mobile.presentation.ui.helpers.LightningInvoiceValidation
import network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class BitcoinLnAddressFieldType {
    Bitcoin,
    Lightning,
}

@Composable
fun BitcoinLnAddressField(
    label: String = "",
    value: String,
    onValueChange: ((String, Boolean) -> Unit)? = null,
    disabled: Boolean = false,
    type: BitcoinLnAddressFieldType = BitcoinLnAddressFieldType.Bitcoin,
    modifier: Modifier = Modifier,
    onBarcodeClick: (() -> Unit)? = null,
    triggerValidation: Int? = null
) {
    
    val validationLogic = remember(type) {
        when (type) {
            BitcoinLnAddressFieldType.Bitcoin -> { input: String ->
                if (BitcoinAddressValidation.validateAddress(input)) null
                else "validation.invalidBitcoinAddress".i18n()
            }
            BitcoinLnAddressFieldType.Lightning -> { input: String ->
                if (LightningInvoiceValidation.validateInvoice(input)) null
                else "validation.invalidLightningInvoice".i18n()
            }
        }
    }

    // Store validation function in mutableStateOf so we can replace it with new function instances.
    // BisqTextField's LaunchedEffect(validation) only triggers when the validation function reference changes.
    // By wrapping the validationLogic in a new lambda { input -> validationLogic(input) }, we create
    // a new function instance each time, which triggers BisqTextField's validation LaunchedEffect.
    var validationError by remember(type) {
        mutableStateOf({ input: String -> validationLogic(input) })
    }

    val helperText = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp".i18n()

    LaunchedEffect(triggerValidation) {
        validationError = { input: String -> validationLogic(input) }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            disabled = disabled,
            showPaste = true,
            modifier = modifier.weight(1f),
            helperText = helperText,
            validation = validationError,
        )
        if (!disabled) {
            Column {
                // a little hack to align the button with input
                if (label.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BisqText.baseLight(
                            text = " ",
                            color = Color.Transparent,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
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
fun BitcoinLnAddressFieldPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "",
                onBarcodeClick = { }
            )
        }
    }
}

@Preview
@Composable
fun BitcoinLnAddressFieldWithLabelPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "",
                label = "Test",
                onBarcodeClick = { }
            )
        }
    }
}

@Preview
@Composable
fun BitcoinLnAddressFieldInvalidPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(
                value = "Test",
                onBarcodeClick = { }
            )
        }
    }
}