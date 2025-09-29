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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ScanQrIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BarcodeScannerDialog
import network.bisq.mobile.presentation.ui.helpers.BitcoinAddressValidation
import network.bisq.mobile.presentation.ui.helpers.BitcoinLightningNormalization
import network.bisq.mobile.presentation.ui.helpers.LightningInvoiceValidation
import network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
import network.bisq.mobile.presentation.ui.helpers.rememberCameraPermissionLauncher
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
    modifier: Modifier = Modifier
) {
    var showScanner by remember { mutableStateOf(false) }

    var shouldBlurAfterFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val permLauncher = rememberCameraPermissionLauncher {
        if (it) {
            showScanner = true
        }
    }

    // If you have not set up a wallet yet, you can find help at the wallet guide
    val helperText = "bisqEasy.tradeState.info.buyer.phase1a.bitcoinPayment.walletHelp".i18n()

    val validationError: (String) -> String? = remember(type) {
        {
            when (type) {
                BitcoinLnAddressFieldType.Bitcoin -> {
                    if (BitcoinAddressValidation.validateAddress(it)) null
                    else "validation.invalidBitcoinAddress".i18n()
                }

                BitcoinLnAddressFieldType.Lightning -> {
                    if (LightningInvoiceValidation.validateInvoice(it)) null
                    else "validation.invalidLightningInvoice".i18n()
                }
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onCanceled = { showScanner = false },
            onFailed = { showScanner = false },
        ) {
            // Normalize + clean: remove scheme (case-insensitive), drop leading slashes, strip query/fragment
            val cleaned = BitcoinLightningNormalization.cleanForValidation(it.data)
            showScanner = false
            if (validationError(cleaned) == null) {
                onValueChange?.invoke(cleaned, true)
            } else {
                onValueChange?.invoke(cleaned, false)
            }
            // trigger input validator
            shouldBlurAfterFocus = true
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isFocused, shouldBlurAfterFocus) {
        if (isFocused && shouldBlurAfterFocus) {
            shouldBlurAfterFocus = false
            focusManager.clearFocus(force = true)
        }
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
            modifier = modifier.weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
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
                    onClick = permLauncher::launch,
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
            BitcoinLnAddressField(value = "")
        }
    }
}

@Preview
@Composable
fun BitcoinLnAddressFieldWithLabelPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(value = "", label = "Test")
        }
    }
}

@Preview
@Composable
fun BitcoinLnAddressFieldInvalidPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BitcoinLnAddressField(value = "Test")
        }
    }
}