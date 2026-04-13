package network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountMethodIcon
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CryptoPaymentMethodCard(
    paymentMethod: CryptoPaymentMethodVO,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
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
            Column {
                BisqText.BaseRegular(paymentMethod.code)
                BisqText.BaseRegularGrey(paymentMethod.name)
            }
        }
    }
}

private fun previewCryptoPaymentMethod(
    paymentMethod: PaymentMethodVO = PaymentMethodVO.XMR,
    code: String = "XMR",
    name: String = "Monero",
): CryptoPaymentMethodVO =
    CryptoPaymentMethodVO(
        paymentMethod = paymentMethod,
        code = code,
        name = name,
    )

@Preview
@Composable
private fun CryptoPaymentMethodCardPreview_XmrPreview() {
    BisqTheme.Preview {
        CryptoPaymentMethodCard(
            paymentMethod = previewCryptoPaymentMethod(),
        )
    }
}

@Preview
@Composable
private fun CryptoPaymentMethodCardPreview_LightningBtcPreview() {
    BisqTheme.Preview {
        CryptoPaymentMethodCard(
            paymentMethod =
                previewCryptoPaymentMethod(
                    paymentMethod = PaymentMethodVO.LNBTC,
                    code = "LN-BTC",
                    name = "Lightning Bitcoin",
                ),
        )
    }
}
