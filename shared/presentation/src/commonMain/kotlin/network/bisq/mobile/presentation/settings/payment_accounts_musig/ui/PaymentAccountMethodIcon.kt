package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.platform.CUSTOM_PAYMENT_BACKGROUND_COLORS
import network.bisq.mobile.presentation.common.ui.platform.customPaymentOverlayLetterColor
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.customPaymentIconIndex
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PaymentAccountMethodIcon(
    paymentMethod: PaymentMethodVO,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (paymentMethod.icon == null) {
            val fallbackIndex = customPaymentIconIndex(paymentMethod.name, CUSTOM_PAYMENT_BACKGROUND_COLORS.size)
            val bgColor = CUSTOM_PAYMENT_BACKGROUND_COLORS[fallbackIndex]
            val overlayLetter = paymentMethod.name.firstOrNull()?.uppercase() ?: "?"

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(bgColor),
            )
            BisqText.BaseBold(
                text = overlayLetter,
                color =
                    customPaymentOverlayLetterColor(
                        darkColor = BisqTheme.colors.dark_grey20,
                        lightColor = BisqTheme.colors.white,
                    ),
                textAlign = TextAlign.Center,
            )
        } else {
            Image(
                painter = painterResource(paymentMethod.icon),
                contentDescription = paymentMethod.name,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(BisqUIConstants.BorderRadius)),
            )
        }
    }
}

@Preview
@Composable
private fun PaymentAccountMethodIcon_FallbackPreview() {
    BisqTheme.Preview {
        PaymentAccountMethodIcon(paymentMethod = PaymentMethodVO.SEPA)
    }
}

@Preview
@Composable
private fun PaymentAccountMethodIcon_RealIconPreview() {
    BisqTheme.Preview {
        PaymentAccountMethodIcon(paymentMethod = PaymentMethodVO.WISE)
    }
}
