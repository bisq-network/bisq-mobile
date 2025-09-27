package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.ScanIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BarcodeScannerDialog
import network.bisq.mobile.presentation.ui.helpers.BitcoinAddressValidation
import network.bisq.mobile.presentation.ui.helpers.LightningInvoiceValidation
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var showScanner by remember { mutableStateOf(false) }
    var shouldBlurAfterFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

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
            showScanner = false
            if (validationError(it.data) == null) {
                onValueChange?.invoke(it.data, true)
            } else {
                onValueChange?.invoke(it.data, false)
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

    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        disabled = disabled,
        showPaste = true,
        modifier = modifier.focusRequester(focusRequester).onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        },
        helperText = helperText,
        validation = validationError,
        rightSuffixModifier = Modifier,
        rightSuffix = {
            BisqIconButton(onClick = { showScanner = true }) {
                ScanIcon(Modifier.size(BisqUIConstants.ScreenPadding2X))
            }
        }
    )

}
