package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction

@Composable
fun CommonCryptoFormSection(
    cryptoUiState: CryptoAccountFormUiState,
    onAction: (AccountFormUiAction) -> Unit,
    showAddress: Boolean,
    showAutoConf: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showAddress) {
            BisqTextFieldV0(
                value = cryptoUiState.addressEntry.value,
                onValueChange = { onAction(CryptoAccountFormUiAction.OnAddressChange(it)) },
                label = "paymentAccounts.crypto.address.address".i18n(),
                placeholder =
                    "paymentAccounts.crypto.address.address.prompt".i18n(),
                isError = cryptoUiState.addressEntry.errorMessage != null,
                bottomMessage = cryptoUiState.addressEntry.errorMessage,
                singleLine = true,
            )
        }

        BisqSwitch(
            checked = cryptoUiState.isInstant,
            modifier = Modifier.padding(top = 12.dp),
            label = "paymentAccounts.crypto.address.isInstant".i18n(),
            onSwitch = { onAction(CryptoAccountFormUiAction.OnIsInstantChange(it)) },
        )

        if (showAutoConf) {
            BisqSwitch(
                checked = cryptoUiState.isAutoConf,
                modifier = Modifier.padding(top = 12.dp),
                label = "paymentAccounts.crypto.address.autoConf.use".i18n(),
                onSwitch = { onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(it)) },
            )

            if (cryptoUiState.isAutoConf) {
                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = cryptoUiState.autoConfNumConfirmationsEntry.value,
                    onValueChange = { onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange(it)) },
                    label = "paymentAccounts.crypto.address.autoConf.numConfirmations".i18n(),
                    placeholder = "paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n(),
                    isError = cryptoUiState.autoConfNumConfirmationsEntry.errorMessage != null,
                    bottomMessage = cryptoUiState.autoConfNumConfirmationsEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = cryptoUiState.autoConfMaxTradeAmountEntry.value,
                    onValueChange = { onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange(it)) },
                    label = "paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n(),
                    placeholder = "paymentAccounts.crypto.address.autoConf.maxTradeAmount.prompt".i18n(),
                    isError = cryptoUiState.autoConfMaxTradeAmountEntry.errorMessage != null,
                    bottomMessage = cryptoUiState.autoConfMaxTradeAmountEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = cryptoUiState.autoConfExplorerUrlsEntry.value,
                    onValueChange = { onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange(it)) },
                    label = "paymentAccounts.crypto.address.autoConf.explorerUrls".i18n(),
                    placeholder = "paymentAccounts.crypto.address.autoConf.explorerUrls.prompt".i18n(),
                    isError = cryptoUiState.autoConfExplorerUrlsEntry.errorMessage != null,
                    bottomMessage =
                        cryptoUiState.autoConfExplorerUrlsEntry.errorMessage
                            ?: "paymentAccounts.crypto.address.autoConf.explorerUrls.help".i18n(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CommonCryptoFormSection_DefaultPreview() {
    BisqTheme.Preview {
        CommonCryptoFormSection(
            cryptoUiState =
                CryptoAccountFormUiState(
                    addressEntry = DataEntry(value = "0x1234567890abcdef1234567890abcdef12345678"),
                    isInstant = true,
                    isAutoConf = false,
                ),
            onAction = {},
            showAddress = true,
            showAutoConf = false,
        )
    }
}

@Preview
@Composable
private fun CommonCryptoFormSection_AutoConfEnabledPreview() {
    BisqTheme.Preview {
        CommonCryptoFormSection(
            cryptoUiState =
                CryptoAccountFormUiState(
                    addressEntry = DataEntry(value = "0x1234567890abcdef1234567890abcdef12345678"),
                    isInstant = false,
                    isAutoConf = true,
                    autoConfNumConfirmationsEntry = DataEntry(value = "2"),
                    autoConfMaxTradeAmountEntry = DataEntry(value = "1"),
                    autoConfExplorerUrlsEntry = DataEntry(value = "https://explorer.example.com"),
                ),
            onAction = {},
            showAddress = true,
            showAutoConf = true,
        )
    }
}

@Preview
@Composable
private fun CommonCryptoFormSection_AddressHiddenPreview() {
    BisqTheme.Preview {
        CommonCryptoFormSection(
            cryptoUiState =
                CryptoAccountFormUiState(
                    isInstant = false,
                    isAutoConf = true,
                    autoConfNumConfirmationsEntry = DataEntry(value = "3"),
                    autoConfMaxTradeAmountEntry = DataEntry(value = "1000"),
                    autoConfExplorerUrlsEntry = DataEntry(value = "https://hidden-address.example"),
                ),
            onAction = {},
            showAddress = false,
            showAutoConf = true,
        )
    }
}
