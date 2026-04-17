package network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.AccountFlowDialog
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountMethodIcon

@Composable
fun FiatPaymentMethodCard(
    paymentMethod: FiatPaymentMethodVO,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    var isCurrencyTruncated by remember(paymentMethod.supportedCurrencyCodes) { mutableStateOf(false) }
    var isCountryTruncated by remember(paymentMethod.countryNames) { mutableStateOf(false) }
    val showCurrencyDialog = remember { mutableStateOf(false) }
    val showCountryDialog = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) BisqTheme.colors.primaryDim.copy(alpha = 0.5f) else BisqTheme.colors.dark_grey40,
        border = if (isSelected) BorderStroke(1.dp, BisqTheme.colors.primary) else null,
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .padding(BisqUIConstants.ScreenPadding)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentAccountMethodIcon(
                paymentMethod = paymentMethod.paymentMethod,
                size = BisqUIConstants.ScreenPadding2X,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.BaseRegular(paymentMethod.name)
                Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPaddingQuarter))
                BisqText.StyledText(
                    text = paymentMethod.supportedCurrencyCodes,
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
                        if (isCurrencyTruncated) {
                            Modifier.clickable {
                                showCurrencyDialog.value = true
                            }
                        } else {
                            Modifier
                        },
                )
                Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPaddingQuarter))
                BisqText.StyledText(
                    text = paymentMethod.countryNames,
                    style =
                        if (isCountryTruncated) {
                            BisqTheme.typography.smallLight.copy(textDecoration = TextDecoration.Underline)
                        } else {
                            BisqTheme.typography.smallLight
                        },
                    color = if (isCountryTruncated) BisqTheme.colors.yellow else BisqTheme.colors.mid_grey30,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        isCountryTruncated = textLayoutResult.hasVisualOverflow
                    },
                    modifier =
                        if (isCountryTruncated) {
                            Modifier.clickable {
                                showCountryDialog.value = true
                            }
                        } else {
                            Modifier
                        },
                )
            }

            paymentMethod.chargebackRisk?.let { chargebackRisk ->
                Surface(
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                    color = chargebackRisk.backgroundColor.copy(alpha = 0.15f),
                ) {
                    BisqText.SmallRegular(
                        text = chargebackRisk.textKey.i18n(),
                        color = chargebackRisk.backgroundColor,
                        modifier =
                            Modifier.padding(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingQuarter,
                            ),
                    )
                }
            }
        }
    }

    if (showCurrencyDialog.value) {
        AccountFlowDialog(
            title = "paymentAccounts.createAccount.paymentMethod.table.currencies".i18n(),
            bodyText = paymentMethod.supportedCurrencyCodes,
            onDismissRequest = { showCurrencyDialog.value = false },
        )
    }

    if (showCountryDialog.value) {
        AccountFlowDialog(
            title = "paymentAccounts.createAccount.paymentMethod.table.countries".i18n(),
            bodyText = paymentMethod.countryNames,
            onDismissRequest = { showCountryDialog.value = false },
        )
    }
}

private val previewFiatPaymentMethod =
    FiatPaymentMethodVO(
        paymentMethod = PaymentMethodVO.ZELLE,
        name = "Zelle",
        supportedCurrencyCodes = "USD",
        countryNames = "United States, United States, United States, United States, United States, United States, United States",
        chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW,
    )

@Preview
@Composable
private fun FiatPaymentMethodCardPreview() {
    BisqTheme.Preview {
        FiatPaymentMethodCard(previewFiatPaymentMethod)
    }
}
