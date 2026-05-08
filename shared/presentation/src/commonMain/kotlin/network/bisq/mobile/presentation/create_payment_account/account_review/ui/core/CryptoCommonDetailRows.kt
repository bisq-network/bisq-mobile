package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

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
