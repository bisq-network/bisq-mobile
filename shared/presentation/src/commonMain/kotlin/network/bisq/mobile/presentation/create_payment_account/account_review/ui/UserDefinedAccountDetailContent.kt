package network.bisq.mobile.presentation.create_payment_account.account_review.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.model.account.toVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailDetailsSection
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.FiatChargebackRiskBadge
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

@Composable
fun UserDefinedAccountDetailContent(
    account: UserDefinedFiatAccount,
) {
    val chargebackRisk =
        remember(account.accountPayload.chargebackRisk) {
            account.accountPayload.chargebackRisk?.toVO()
        }

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
                    paymentType = PaymentTypeVO.CUSTOM,
                    size = BisqUIConstants.ScreenPadding2X,
                )
                Column {
                    BisqText.BaseRegular(account.accountPayload.paymentMethodName)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountDetailFieldRow(
                    label = "paymentAccounts.userDefined.accountData".i18n(),
                    value = account.accountPayload.accountData,
                )

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )

                chargebackRisk?.let { risk ->
                    BisqGap.VQuarter()
                    FiatChargebackRiskBadge(risk = risk)
                }
            }
        }
    }
}

private val previewAccount =
    UserDefinedFiatAccount(
        accountName = "Custom Bank Account",
        accountPayload =
            UserDefinedFiatAccountPayload(
                accountData = "IBAN: DE89370400440532013000",
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                paymentMethodName = "CUSTOM",
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "1000 EUR",
        tradeDuration = "8 days",
    )

private val previewAccountWithLongCountryAndCurrency =
    UserDefinedFiatAccount(
        accountName = "Custom International Account",
        accountPayload =
            UserDefinedFiatAccountPayload(
                accountData = "Use SWIFT: DEUTDEFF and reference code 1234567890",
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                paymentMethodName = "CUSTOM",
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "1000 EUR",
        tradeDuration = "8 days",
    )

@Preview
@Composable
private fun UserDefinedAccountDetailContentPreview() {
    BisqTheme.Preview {
        UserDefinedAccountDetailContent(
            account = previewAccount,
        )
    }
}

@Preview
@Composable
private fun UserDefinedAccountDetailContent_LongCountryCurrencyPreview() {
    BisqTheme.Preview {
        UserDefinedAccountDetailContent(
            account = previewAccountWithLongCountryAndCurrency,
        )
    }
}
