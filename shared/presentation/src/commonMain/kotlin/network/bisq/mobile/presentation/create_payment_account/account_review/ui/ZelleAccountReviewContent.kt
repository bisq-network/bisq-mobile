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
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountReviewFieldRow
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.FiatChargebackRiskBadge
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

@Composable
fun ZelleAccountReviewContent(
    paymentMethod: FiatPaymentMethodVO,
    account: ZelleAccount,
) {
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
                    BisqText.BaseRegularGrey(paymentMethod.supportedCurrencyCodes)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountReviewFieldRow(
                    label = "paymentAccounts.country".i18n(),
                    value = paymentMethod.countryNames,
                )

                AccountReviewFieldRow(
                    label = "paymentAccounts.holderName".i18n(),
                    value = account.accountPayload.holderName,
                )

                AccountReviewFieldRow(
                    label = "paymentAccounts.emailOrMobileNr".i18n(),
                    value = account.accountPayload.emailOrMobileNr,
                )

                if (paymentMethod.restrictions.isNotEmpty()) {
                    AccountReviewFieldRow(
                        label = "paymentAccounts.restrictions".i18n(),
                        value = paymentMethod.restrictions,
                    )
                }

                paymentMethod.chargebackRisk?.let { risk ->
                    BisqGap.VQuarter()
                    FiatChargebackRiskBadge(risk = risk)
                }
            }
        }
    }
}

private val previewPaymentMethod =
    FiatPaymentMethodVO(
        paymentType = PaymentTypeVO.ZELLE,
        name = "Zelle",
        supportedCurrencyCodes = "USD",
        countryNames = "United States",
        chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW,
        restrictions = "Max. trade amount: 5000.00 / Max. trade duration: 4 days",
    )

private val previewAccount =
    ZelleAccount(
        accountName = "Alice Doe",
        accountPayload =
            ZelleAccountPayload(
                holderName = "Alice Doe",
                emailOrMobileNr = "alice@example.com",
            ),
    )

@Preview
@Composable
private fun ZelleAccountReviewContentPreview() {
    BisqTheme.Preview {
        ZelleAccountReviewContent(
            paymentMethod = previewPaymentMethod,
            account = previewAccount,
        )
    }
}
