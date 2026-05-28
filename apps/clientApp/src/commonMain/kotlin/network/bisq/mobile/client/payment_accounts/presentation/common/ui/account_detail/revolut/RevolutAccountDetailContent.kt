package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.revolut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.ExpandableAccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.FiatChargebackRiskBadge
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun RevolutAccountDetailContent(
    account: RevolutAccount,
) {
    val revolutDetail = remember(account) { account.toDetailVO() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            AccountDetailHeader(
                paymentType = PaymentTypeVO.REVOLUT,
                primaryText = account.accountPayload.paymentMethodName,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountDetailFieldRow(
                    label = "paymentAccounts.userName".i18n(),
                    value = account.accountPayload.userName,
                )

                ExpandableAccountDetailFieldRow(
                    label = "mobile.paymentAccounts.currencyPicker.title".i18n(),
                    value = revolutDetail.selectedCurrencies,
                )

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )

                revolutDetail.chargebackRisk?.let { risk ->
                    BisqGap.VQuarter()
                    FiatChargebackRiskBadge(risk = risk)
                }
            }
        }
    }
}

private val previewAccount =
    RevolutAccount(
        accountName = "Revolut Main",
        accountPayload =
            RevolutAccountPayload(
                selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US dollar"), FiatCurrency(code = "EUR", name = "Euro")),
                userName = "satoshi",
                paymentMethodName = "Revolut",
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            ),
        tradeDuration = "4 days",
        tradeLimitInfo = "5000.00",
        creationDate = "Apr 3, 2026",
    )

@Preview
@Composable
private fun RevolutAccountDetailContentPreview() {
    BisqTheme.Preview {
        RevolutAccountDetailContent(account = previewAccount)
    }
}
