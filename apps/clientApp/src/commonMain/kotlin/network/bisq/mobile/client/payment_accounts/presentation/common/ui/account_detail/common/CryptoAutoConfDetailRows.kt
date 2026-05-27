package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common

import androidx.compose.runtime.Composable
import network.bisq.mobile.i18n.i18n

@Composable
fun CryptoAutoConfDetailRows(
    isAutoConf: Boolean,
    autoConfNumConfirmations: Int?,
    autoConfMaxTradeAmount: Long?,
    autoConfExplorerUrls: String?,
) {
    AccountDetailFieldRow(
        label = "paymentAccounts.crypto.address.autoConf.use".i18n(),
        value = if (isAutoConf) "state.enabled".i18n() else "state.disabled".i18n(),
    )

    if (isAutoConf) {
        autoConfNumConfirmations?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.crypto.address.autoConf.numConfirmations".i18n(),
                value = it.toString(),
            )
        }
        autoConfMaxTradeAmount?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n(),
                value = it.toString(),
            )
        }
        autoConfExplorerUrls?.let {
            AccountDetailFieldRow(
                label = "paymentAccounts.crypto.address.autoConf.explorerUrls".i18n(),
                value = it,
            )
        }
    }
}
