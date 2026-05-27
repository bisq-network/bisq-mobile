package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n

@Composable
fun CryptoCommonDetailRows(
    address: String,
    isInstant: Boolean,
    showAddress: Boolean,
) {
    if (showAddress) {
        AccountDetailFieldRow(
            label = "paymentAccounts.crypto.address.address".i18n(),
            value = address,
        )
    }

    AccountDetailFieldRow(
        label = "paymentAccounts.crypto.address.isInstant".i18n(),
        value = if (isInstant) "state.enabled".i18n() else "state.disabled".i18n(),
    )
}
