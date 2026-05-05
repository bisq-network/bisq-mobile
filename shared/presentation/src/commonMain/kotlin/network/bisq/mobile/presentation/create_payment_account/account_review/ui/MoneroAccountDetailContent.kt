package network.bisq.mobile.presentation.create_payment_account.account_review.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailDetailsSection
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

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
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .padding(BisqUIConstants.ScreenPadding)
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                PaymentAccountTypeIcon(
                    paymentType = PaymentTypeVO.XMR,
                    size = BisqUIConstants.ScreenPadding2X,
                )
                Column {
                    BisqText.BaseRegular(account.accountPayload.currencyName)
                    BisqText.BaseRegularGrey(account.accountPayload.currencyCode)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                if (!payload.useSubAddresses) {
                    AccountDetailFieldRow(
                        label = "paymentAccounts.crypto.address.address".i18n(),
                        value = payload.address,
                    )
                }

                AccountDetailFieldRow(
                    label = "paymentAccounts.crypto.address.isInstant".i18n(),
                    value = if (payload.isInstant) "state.enabled".i18n() else "state.disabled".i18n(),
                )

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

                if (account.accountPayload.supportAutoConf) {
                    val isAutoConf = payload.isAutoConf == true
                    AccountDetailFieldRow(
                        label = "paymentAccounts.crypto.address.autoConf.use".i18n(),
                        value = if (isAutoConf) "state.enabled".i18n() else "state.disabled".i18n(),
                    )

                    if (isAutoConf) {
                        payload.autoConfNumConfirmations?.let {
                            AccountDetailFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.numConfirmations".i18n(),
                                value = it.toString(),
                            )
                        }
                        payload.autoConfMaxTradeAmount?.let {
                            AccountDetailFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n(),
                                value = it.toString(),
                            )
                        }
                        payload.autoConfExplorerUrls?.let {
                            AccountDetailFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.explorerUrls".i18n(),
                                value = it,
                            )
                        }
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
