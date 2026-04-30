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
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountReviewFieldRow
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

@Composable
fun MoneroAccountReviewContent(
    paymentMethod: CryptoPaymentMethodVO,
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
                    paymentType = paymentMethod.paymentType,
                    size = BisqUIConstants.ScreenPadding2X,
                )
                Column {
                    BisqText.BaseRegular(paymentMethod.name)
                    BisqText.BaseRegularGrey(paymentMethod.code)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                if (!payload.useSubAddresses) {
                    AccountReviewFieldRow(
                        label = "paymentAccounts.crypto.address.address".i18n(),
                        value = payload.address,
                    )
                }

                AccountReviewFieldRow(
                    label = "paymentAccounts.crypto.address.isInstant".i18n(),
                    value = if (payload.isInstant) "state.enabled".i18n() else "state.disabled".i18n(),
                )

                AccountReviewFieldRow(
                    label = "paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n(),
                    value = if (payload.useSubAddresses) "state.enabled".i18n() else "state.disabled".i18n(),
                )

                if (payload.useSubAddresses) {
                    payload.mainAddress?.let {
                        AccountReviewFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.mainAddresses".i18n(),
                            value = it,
                        )
                    }
                    payload.privateViewKey?.let {
                        AccountReviewFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.privateViewKey".i18n(),
                            value = it,
                        )
                    }
                    payload.accountIndex?.let {
                        AccountReviewFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.accountIndex".i18n(),
                            value = it.toString(),
                        )
                    }
                    payload.initialSubAddressIndex?.let {
                        AccountReviewFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n(),
                            value = it.toString(),
                        )
                    }
                    payload.subAddress?.let {
                        AccountReviewFieldRow(
                            label = "paymentAccounts.crypto.address.xmr.subAddress".i18n(),
                            value = it,
                        )
                    }
                }

                if (paymentMethod.supportAutoConf) {
                    val isAutoConf = payload.isAutoConf == true
                    AccountReviewFieldRow(
                        label = "paymentAccounts.crypto.address.autoConf.use".i18n(),
                        value = if (isAutoConf) "state.enabled".i18n() else "state.disabled".i18n(),
                    )

                    if (isAutoConf) {
                        payload.autoConfNumConfirmations?.let {
                            AccountReviewFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.numConfirmations".i18n(),
                                value = it.toString(),
                            )
                        }
                        payload.autoConfMaxTradeAmount?.let {
                            AccountReviewFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n(),
                                value = it.toString(),
                            )
                        }
                        payload.autoConfExplorerUrls?.let {
                            AccountReviewFieldRow(
                                label = "paymentAccounts.crypto.address.autoConf.explorerUrls".i18n(),
                                value = it,
                            )
                        }
                    }
                }

                if (paymentMethod.restrictions.isNotEmpty()) {
                    AccountReviewFieldRow(
                        label = "paymentAccounts.restrictions".i18n(),
                        value = paymentMethod.restrictions,
                    )
                }
            }
        }
    }
}

private val previewPaymentMethod =
    CryptoPaymentMethodVO(
        paymentType = PaymentTypeVO.XMR,
        code = "XMR",
        name = "Monero",
        supportAutoConf = true,
        restrictions = "Max. trade amount: 0.02 XMR / Max. trade duration: 1 day",
    )

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
            ),
        creationDate = null,
        tradeLimitInfo = null,
        tradeDuration = null,
    )

@Preview
@Composable
private fun MoneroAccountReviewContentPreview() {
    BisqTheme.Preview {
        MoneroAccountReviewContent(
            paymentMethod = previewPaymentMethod,
            account = previewAccount,
        )
    }
}
