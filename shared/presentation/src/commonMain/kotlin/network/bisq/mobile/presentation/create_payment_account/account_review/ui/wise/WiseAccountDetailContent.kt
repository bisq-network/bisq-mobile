package network.bisq.mobile.presentation.create_payment_account.account_review.ui.wise

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
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailDetailsSection
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.ExpandableAccountDetailFieldRow
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.FiatChargebackRiskBadge
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

@Composable
fun WiseAccountDetailContent(
    account: WiseAccount,
) {
    val wiseDetail = remember(account) { account.toDetailVO() }

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
                    paymentType = PaymentTypeVO.WISE,
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
                    label = "paymentAccounts.holderName".i18n(),
                    value = account.accountPayload.holderName,
                )

                AccountDetailFieldRow(
                    label = "paymentAccounts.email".i18n(),
                    value = account.accountPayload.email,
                )

                ExpandableAccountDetailFieldRow(
                    label = "mobile.paymentAccounts.wise.picker.title".i18n(),
                    value = wiseDetail.selectedCurrencies,
                )

                AccountDetailDetailsSection(
                    creationDate = account.creationDate,
                    tradeLimitInfo = account.tradeLimitInfo,
                    tradeDuration = account.tradeDuration,
                )

                wiseDetail.chargebackRisk?.let { risk ->
                    BisqGap.VQuarter()
                    FiatChargebackRiskBadge(risk = risk)
                }
            }
        }
    }
}

private val previewAccount =
    WiseAccount(
        accountName = "Wise Main",
        accountPayload =
            WiseAccountPayload(
                selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US dollar"), FiatCurrency(code = "EUR", name = "Euro")),
                holderName = "Satoshi Nakamoto",
                email = "satoshi@example.com",
                paymentMethodName = "Wise",
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            ),
        tradeDuration = "4 days",
        tradeLimitInfo = "5000.00",
        creationDate = "Apr 3, 2026",
    )

@Preview
@Composable
private fun WiseAccountDetailContentPreview() {
    BisqTheme.Preview {
        WiseAccountDetailContent(account = previewAccount)
    }
}
