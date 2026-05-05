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
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
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
fun ZelleAccountDetailContent(
    account: ZelleAccount,
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
                    paymentType = PaymentTypeVO.ZELLE,
                    size = BisqUIConstants.ScreenPadding2X,
                )
                Column {
                    BisqText.BaseRegular(account.accountPayload.paymentMethodName)
                    BisqText.BaseRegularGrey(account.accountPayload.currency)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                account.accountPayload.country?.let { country ->
                    AccountDetailFieldRow(
                        label = "paymentAccounts.country".i18n(),
                        value = country,
                    )
                }

                AccountDetailFieldRow(
                    label = "paymentAccounts.holderName".i18n(),
                    value = account.accountPayload.holderName,
                )

                AccountDetailFieldRow(
                    label = "paymentAccounts.emailOrMobileNr".i18n(),
                    value = account.accountPayload.emailOrMobileNr,
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
    ZelleAccount(
        accountName = "Alice Doe",
        accountPayload =
            ZelleAccountPayload(
                holderName = "Alice Doe",
                emailOrMobileNr = "alice@example.com",
                paymentMethodName = "Zelle",
                currency = "USD",
                country = "United States",
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            ),
        tradeDuration = "8 days",
        tradeLimitInfo = "1000 USD",
        creationDate = "Apr 3, 2026",
    )

@Preview
@Composable
private fun ZelleAccountDetailContentPreview() {
    BisqTheme.Preview {
        ZelleAccountDetailContent(
            account = previewAccount,
        )
    }
}
