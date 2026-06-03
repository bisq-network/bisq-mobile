package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.sepa

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
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.ExpandableAccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.FiatChargebackRiskBadge
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun SepaAccountDetailContent(account: SepaAccount) {
    val acceptedCountries =
        remember(account.accountPayload.acceptedCountries) {
            account.accountPayload.acceptedCountries
                .sortedBy { country -> country.name }
                .joinToString { country -> country.name }
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            AccountDetailHeader(
                paymentType = PaymentTypeVO.SEPA,
                primaryText = account.accountPayload.paymentMethodName,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountDetailFieldRow(
                    label = "paymentAccounts.createAccount.accountData.country".i18n(),
                    value = account.accountPayload.country.name,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.currency".i18n(),
                    value = account.accountPayload.currency.toDisplayString(),
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.holderName".i18n(),
                    value = account.accountPayload.holderName,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.sepa.iban".i18n(),
                    value = account.accountPayload.iban,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.sepa.bic".i18n(),
                    value = account.accountPayload.bic,
                )
                ExpandableAccountDetailFieldRow(
                    label = "paymentAccounts.createAccount.accountData.sepa.acceptCountries".i18n(),
                    value = acceptedCountries,
                )

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )

                account.accountPayload.chargebackRisk?.let { risk ->
                    BisqGap.VQuarter()
                    risk.toVO()?.let { riskVO -> FiatChargebackRiskBadge(risk = riskVO) }
                }
            }
        }
    }
}

private val previewAccount =
    SepaAccount(
        accountName = "SEPA Main",
        accountPayload =
            SepaAccountPayload(
                chargebackRisk = FiatPaymentMethodChargebackRisk.VERY_LOW,
                paymentMethodName = "SEPA",
                currency = FiatCurrency("EUR", "Euro"),
                country = Country("DE", "Germany"),
                acceptedCountries = listOf(Country("DE", "Germany"), Country("FR", "France"), Country("ES", "Spain")),
                holderName = "Alice Doe",
                iban = "DE89370400440532013000",
                bic = "DEUTDEFF",
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "5000.00 EUR",
        tradeDuration = "5 days",
    )

@Preview
@Composable
private fun SepaAccountDetailContentPreview() {
    BisqTheme.Preview {
        SepaAccountDetailContent(previewAccount)
    }
}
