package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.ach_transfer

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
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.FiatChargebackRiskBadge
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun AchTransferAccountDetailContent(account: AchTransferAccount) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            AccountDetailHeader(
                paymentType = PaymentTypeVO.ACH_TRANSFER,
                primaryText = account.accountPayload.paymentMethodName,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountDetailFieldRow(
                    label = "paymentAccounts.country".i18n(),
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
                    label = "paymentAccounts.holderAddress".i18n(),
                    value = account.accountPayload.holderAddress,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.bank.bankName".i18n(),
                    value = account.accountPayload.bankName,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.achTransfer.routingNr".i18n(),
                    value = account.accountPayload.routingNr,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.accountNr".i18n(),
                    value = account.accountPayload.accountNr,
                )
                AccountDetailFieldRow(
                    label = "paymentAccounts.bank.bankAccountType".i18n(),
                    value = account.accountPayload.bankAccountType.toDisplayString(),
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
    AchTransferAccount(
        accountName = "ACH Main",
        accountPayload =
            AchTransferAccountPayload(
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                paymentMethodName = "ACH",
                currency = FiatCurrency("USD", "US Dollar"),
                country = Country("US", "United States"),
                holderName = "Alice Doe",
                holderAddress = "123 Main St",
                bankName = "Bisq Bank",
                routingNr = "123456789",
                accountNr = "000123456789",
                bankAccountType = BankAccountType.CHECKING,
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "5000.00 USD",
        tradeDuration = "5 days",
    )

@Preview
@Composable
private fun AchTransferAccountDetailContentPreview() {
    BisqTheme.Preview {
        AchTransferAccountDetailContent(previewAccount)
    }
}
