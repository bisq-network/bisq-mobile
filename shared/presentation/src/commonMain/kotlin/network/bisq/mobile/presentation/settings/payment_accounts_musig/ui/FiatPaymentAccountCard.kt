package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.FiatAccountVO

@Composable
fun FiatPaymentAccountCard(
    account: FiatAccountVO,
    modifier: Modifier = Modifier,
) {
    var isCurrencyTruncated by remember(account.currency) { mutableStateOf(false) }
    val showCurrencyDialog = remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding)
                .fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentAccountMethodIcon(
                paymentMethod = account.paymentMethod,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H4Regular(account.accountName)
                BisqGap.VQuarter()
                Row {
                    BisqText.BaseRegular(
                        text = account.paymentMethodName,
                        color = BisqTheme.colors.mid_grey20,
                    )
                    if (account.country.isNotBlank()) {
                        BisqText.BaseRegular(
                            text = " | ${account.country}",
                            color = BisqTheme.colors.mid_grey20,
                        )
                    }
                }
            }
        }
        if (account.currency.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BisqText.StyledText(
                text = account.currency,
                style =
                    if (isCurrencyTruncated) {
                        BisqTheme.typography.smallLight.copy(textDecoration = TextDecoration.Underline)
                    } else {
                        BisqTheme.typography.smallLight
                    },
                color = if (isCurrencyTruncated) BisqTheme.colors.primary else BisqTheme.colors.mid_grey30,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    isCurrencyTruncated = textLayoutResult.hasVisualOverflow
                },
                modifier =
                    Modifier.clickable(enabled = isCurrencyTruncated) {
                        showCurrencyDialog.value = true
                    },
            )
        }

        account.chargebackRisk?.let { chargebackRisk ->
            Spacer(modifier = Modifier.height(8.dp))
            ChargebackRiskBadge(chargebackRisk)
        }
    }

    if (showCurrencyDialog.value) {
        AccountFlowDialog(
            title = "paymentAccounts.createAccount.paymentMethod.table.currencies".i18n(),
            bodyText = account.currency,
            onDismissRequest = { showCurrencyDialog.value = false },
        )
    }
}

@Composable
private fun ChargebackRiskBadge(risk: FiatPaymentMethodChargebackRiskVO) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = risk.backgroundColor.copy(alpha = 0.12f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            Surface(
                modifier = Modifier.size(width = 3.dp, height = 16.dp),
                shape = RoundedCornerShape(2.dp),
                color = risk.backgroundColor,
            ) {}
            BisqText.SmallRegular(
                risk.textKey.i18n(),
                color = risk.backgroundColor,
            )
        }
    }
}

@ExcludeFromCoverage
private fun previewFiatAccount(
    accountName: String = "My SEPA Account",
    chargebackRisk: FiatPaymentMethodChargebackRiskVO = FiatPaymentMethodChargebackRiskVO.LOW,
    paymentMethod: PaymentMethodVO = PaymentMethodVO.SEPA,
    paymentMethodName: String = "Sepa",
    currency: String = "XPF (CFP Franc), YER (Yemeni Rial), ZAR (South African Rand)",
    country: String = "Germany",
): FiatAccountVO =
    FiatAccountVO(
        accountName = accountName,
        chargebackRisk = chargebackRisk,
        paymentMethod = paymentMethod,
        paymentMethodName = paymentMethodName,
        currency = currency,
        country = country,
    )

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_LowRiskPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account = previewFiatAccount(),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_NoCurrencyAndNoCountryPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "Wise Personal",
                    paymentMethod = PaymentMethodVO.WISE,
                    paymentMethodName = "Wise",
                    currency = "",
                    country = "",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_ModerateRiskLongNamesPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "Primary Household Transfer Account",
                    chargebackRisk = FiatPaymentMethodChargebackRiskVO.MODERATE,
                    paymentMethod = PaymentMethodVO.ZELLE,
                    paymentMethodName = "Zelle",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_VeryLowRiskPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "EU Settlement",
                    chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
                    paymentMethod = PaymentMethodVO.SEPA,
                    paymentMethodName = "Sepa",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_MediumRiskPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "EU Settlement",
                    chargebackRisk = FiatPaymentMethodChargebackRiskVO.MEDIUM,
                    paymentMethod = PaymentMethodVO.SEPA,
                    paymentMethodName = "Sepa",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_CustomPaymentMethodFallbackIconPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "Custom Transfer",
                    paymentMethod = PaymentMethodVO.CUSTOM,
                    paymentMethodName = "Custom",
                    currency = "ARS (Argentine Peso)",
                    country = "Argentina",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_CountryOnlyPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "Country-only account",
                    paymentMethod = PaymentMethodVO.ZELLE,
                    paymentMethodName = "Zelle",
                    currency = "",
                    country = "United States",
                ),
        )
    }
}

@Preview
@Composable
private fun FiatPaymentAccountCardPreview_LongCurrencyClickableHintPreview() {
    BisqTheme.Preview {
        FiatPaymentAccountCard(
            account =
                previewFiatAccount(
                    accountName = "Wide currency list",
                    paymentMethod = PaymentMethodVO.SEPA,
                    paymentMethodName = "Sepa",
                    currency = "SVC (Salvadoran Colón), SYP (Syrian Pound), SZL (Swazi Lilangeni), THB (Thai Baht), TJS (Tajikistani Somoni), TMT (Turkmenistani Manat), TND (Tunisian Dinar), TOP (Tongan Paʻanga), TRY (Turkish Lira), TTD (Trinidad & Tobago Dollar), TWD (New Taiwan Dollar), TZS (Tanzanian Shilling), UAH (Ukrainian Hryvnia), UGX (Ugandan Shilling), USD (US Dollar), UYU (Uruguayan Peso), UZS (Uzbekistani Som), VES (Venezuelan Bolívar), VND (Vietnamese Dong), VUV (Vanuatu Vatu), WST (Samoan Tala), XAF (Central African CFA Franc), XCD (East Caribbean Dollar), XCG (Caribbean Guilder), XOF (West African CFA Franc), XPF (CFP Franc), YER (Yemeni Rial), ZAR (South African Rand), ZMW (Zambian Kwacha), ZWL (Zimbabwean Dollar (2009))",
                    country = "Germany",
                ),
        )
    }
}
