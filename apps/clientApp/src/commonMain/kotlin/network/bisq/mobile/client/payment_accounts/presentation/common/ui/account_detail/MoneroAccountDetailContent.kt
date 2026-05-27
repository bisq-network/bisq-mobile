package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoAccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoAutoConfDetailRows
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.CryptoCommonDetailRows
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun MoneroAccountDetailContent(
    account: MoneroAccount,
) {
    val payload = account.accountPayload

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            CryptoAccountDetailHeader(
                paymentType = PaymentTypeVO.XMR,
                currencyName = payload.currencyName,
                currencyCode = payload.currencyCode,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                CryptoCommonDetailRows(
                    address = payload.address,
                    isInstant = payload.isInstant,
                    showAddress = !payload.useSubAddresses,
                )

                if (payload.supportAutoConf) {
                    CryptoAutoConfDetailRows(
                        isAutoConf = payload.isAutoConf == true,
                        autoConfNumConfirmations = payload.autoConfNumConfirmations,
                        autoConfMaxTradeAmount = payload.autoConfMaxTradeAmount,
                        autoConfExplorerUrls = payload.autoConfExplorerUrls,
                    )
                }

                AccountDetailFieldRow(
                    label = "paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n(),
                    value = if (payload.useSubAddresses) "state.enabled".i18n() else "state.disabled".i18n(),
                )

                if (payload.useSubAddresses) {
                    payload.mainAddress?.let {
                        AccountDetailFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.mainAddresses".i18n(),
                            value = it,
                        )
                    }
                    payload.privateViewKey?.let {
                        AccountDetailFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.privateViewKey".i18n(),
                            value = it,
                        )
                    }
                    payload.accountIndex?.let {
                        AccountDetailFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.accountIndex".i18n(),
                            value = it.toString(),
                        )
                    }
                    payload.initialSubAddressIndex?.let {
                        AccountDetailFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n(),
                            value = it.toString(),
                        )
                    }
                    payload.subAddress?.let {
                        AccountDetailFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.subAddress".i18n(),
                            value = it,
                        )
                    }
                }

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )
            }
        }
    }
}

private val previewAccount =
    MoneroAccount(
        accountName = "My Monero Account",
        accountPayload =
            MoneroAccountPayload(
                address = "",
                isInstant = false,
                isAutoConf = true,
                autoConfNumConfirmations = 10,
                autoConfMaxTradeAmount = 200000,
                autoConfExplorerUrls = "https://example.com/explorer",
                useSubAddresses = true,
                mainAddress = "44AFFq5kSiGBoZ...",
                privateViewKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                subAddress = "89ABCDE...",
                accountIndex = 0,
                initialSubAddressIndex = 0,
                currencyCode = "XMR",
                currencyName = "Monero",
                supportAutoConf = true,
            ),
        creationDate = null,
        tradeLimitInfo = null,
        tradeDuration = null,
    )

@Preview
@Composable
private fun MoneroAccountDetailContentPreview() {
    BisqTheme.Preview {
        MoneroAccountDetailContent(
            account = previewAccount,
        )
    }
}
