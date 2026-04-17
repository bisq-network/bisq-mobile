package network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun FiatChargebackRiskFilterSection(
    activeRiskFilter: FiatPaymentMethodChargebackRiskVO?,
    onRiskFilterChange: (FiatPaymentMethodChargebackRiskVO?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        item {
            RiskFilterChip(
                label = "mobile.user.paymentAccounts.createAccount.chargebackRiskFilter.all".i18n(),
                isSelected = activeRiskFilter == null,
                color = BisqTheme.colors.white,
                onClick = { onRiskFilterChange(null) },
            )
        }
        item {
            RiskFilterChip(
                label = FiatPaymentMethodChargebackRiskVO.VERY_LOW.textKey.i18n(),
                isSelected = activeRiskFilter == FiatPaymentMethodChargebackRiskVO.VERY_LOW,
                color = FiatPaymentMethodChargebackRiskVO.VERY_LOW.backgroundColor,
                onClick = { onRiskFilterChange(FiatPaymentMethodChargebackRiskVO.VERY_LOW) },
            )
        }
        item {
            RiskFilterChip(
                label = FiatPaymentMethodChargebackRiskVO.LOW.textKey.i18n(),
                isSelected = activeRiskFilter == FiatPaymentMethodChargebackRiskVO.LOW,
                color = FiatPaymentMethodChargebackRiskVO.LOW.backgroundColor,
                onClick = { onRiskFilterChange(FiatPaymentMethodChargebackRiskVO.LOW) },
            )
        }
        item {
            RiskFilterChip(
                label = FiatPaymentMethodChargebackRiskVO.MEDIUM.textKey.i18n(),
                isSelected = activeRiskFilter == FiatPaymentMethodChargebackRiskVO.MEDIUM,
                color = FiatPaymentMethodChargebackRiskVO.MEDIUM.backgroundColor,
                onClick = { onRiskFilterChange(FiatPaymentMethodChargebackRiskVO.MEDIUM) },
            )
        }
        item {
            RiskFilterChip(
                label = FiatPaymentMethodChargebackRiskVO.MODERATE.textKey.i18n(),
                isSelected = activeRiskFilter == FiatPaymentMethodChargebackRiskVO.MODERATE,
                color = FiatPaymentMethodChargebackRiskVO.MODERATE.backgroundColor,
                onClick = { onRiskFilterChange(FiatPaymentMethodChargebackRiskVO.MODERATE) },
            )
        }
    }
}

/**
 * A filter chip for risk-level filtering in Step 1.
 *
 * Selected state uses a filled background (color at 20% alpha) + colored text.
 * Unselected state uses dark_grey40 background + mid_grey20 text.
 * Color parameter defaults to white for the "All" chip.
 */
@Composable
private fun RiskFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color = BisqTheme.colors.white,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) color.copy(alpha = 0.22f) else BisqTheme.colors.dark_grey40,
        border = if (isSelected) BorderStroke(1.dp, color) else null,
    ) {
        BisqText.SmallRegular(
            label,
            color = if (isSelected) color else BisqTheme.colors.mid_grey20,
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        )
    }
}

@Preview
@Composable
private fun FiatChargebackRiskFilterSectionPreview_AllSelectedPreview() {
    BisqTheme.Preview {
        FiatChargebackRiskFilterSection(
            activeRiskFilter = null,
            onRiskFilterChange = {},
        )
    }
}

@Preview
@Composable
private fun FiatChargebackRiskFilterSectionPreview_LowSelectedPreview() {
    BisqTheme.Preview {
        FiatChargebackRiskFilterSection(
            activeRiskFilter = FiatPaymentMethodChargebackRiskVO.LOW,
            onRiskFilterChange = {},
        )
    }
}
