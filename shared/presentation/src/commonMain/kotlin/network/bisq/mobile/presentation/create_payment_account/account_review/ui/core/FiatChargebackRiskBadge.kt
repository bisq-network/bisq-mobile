package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun FiatChargebackRiskBadge(risk: FiatPaymentMethodChargebackRiskVO) {
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
                "paymentAccounts.summary.chargebackRisk".i18n(risk.textKey.i18n()),
                color = risk.backgroundColor,
            )
        }
    }
}

@Preview
@Composable
private fun FiatChargebackRiskBadgePreview() {
    BisqTheme.Preview {
        FiatChargebackRiskBadge(risk = FiatPaymentMethodChargebackRiskVO.LOW)
    }
}
