package network.bisq.mobile.presentation.common.model.account

import androidx.compose.ui.graphics.Color
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

enum class FiatPaymentMethodChargebackRiskVO(
    val backgroundColor: Color,
    val textKey: String,
) {
    VERY_LOW(BisqTheme.colors.primary, "paymentAccounts.createAccount.paymentMethod.risk.veryLow"),
    LOW(BisqTheme.colors.warning, "paymentAccounts.createAccount.paymentMethod.risk.low"),
    MEDIUM(BisqTheme.colors.yellow, "paymentAccounts.createAccount.paymentMethod.risk.medium"),
    MODERATE(BisqTheme.colors.danger, "paymentAccounts.createAccount.paymentMethod.risk.moderate"),
}

fun FiatPaymentMethodChargebackRisk?.toVO(): FiatPaymentMethodChargebackRiskVO =
    when (this) {
        FiatPaymentMethodChargebackRisk.VERY_LOW -> FiatPaymentMethodChargebackRiskVO.VERY_LOW
        FiatPaymentMethodChargebackRisk.LOW -> FiatPaymentMethodChargebackRiskVO.LOW
        FiatPaymentMethodChargebackRisk.MEDIUM -> FiatPaymentMethodChargebackRiskVO.MEDIUM
        FiatPaymentMethodChargebackRisk.MODERATE -> FiatPaymentMethodChargebackRiskVO.MODERATE
        else -> FiatPaymentMethodChargebackRiskVO.MODERATE
    }
