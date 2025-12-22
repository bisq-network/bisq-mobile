package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.platform.CUSTOM_PAYMENT_BACKGROUND_COLORS
import network.bisq.mobile.presentation.common.ui.platform.isIOSPlatform
import network.bisq.mobile.presentation.common.ui.utils.customPaymentIconIndex
import network.bisq.mobile.presentation.common.ui.utils.hasKnownPaymentIcon
import network.bisq.mobile.presentation.common.ui.utils.hasKnownSettlementIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

private val CUSTOM_PAYMENT_ICON_IDS = listOf(
    "custom_payment_1",
    "custom_payment_2",
    "custom_payment_3",
    "custom_payment_4",
    "custom_payment_5",
    "custom_payment_6",
)
// TODO: Get params and render apt
@Composable
fun PaymentMethods(
    baseSidePaymentMethods: List<String>,
    quoteSidePaymentMethods: List<String>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            quoteSidePaymentMethods.forEach { paymentMethod ->

                Box(contentAlignment = Alignment.Center) {
                    // Check if the payment method has a known icon file
                    val isMissingIcon = !hasKnownPaymentIcon(paymentMethod)
                    val customIndex = if(isMissingIcon)
                        customPaymentIconIndex(paymentMethod, CUSTOM_PAYMENT_ICON_IDS.size)
                    else
                        0
                    val fallbackPath = "drawable/payment/fiat/${CUSTOM_PAYMENT_ICON_IDS[customIndex]}.png"
                    // For custom payment icons on iOS, use a programmatic colored background
                    if (isMissingIcon && isIOSPlatform()) {
                        val bgColor = CUSTOM_PAYMENT_BACKGROUND_COLORS.getOrElse(customIndex) {
                            CUSTOM_PAYMENT_BACKGROUND_COLORS[0]
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(bgColor, RoundedCornerShape(4.dp))
                        )
                    } else {
                        DynamicImage(
                            path = "drawable/payment/fiat/${
                                paymentMethod
                                    .lowercase()
                                    .replace("-", "_")
                            }.png",
                            contentDescription =  if (isMissingIcon) "mobile.components.paymentMethods.customPaymentMethod".i18n(paymentMethod) else paymentMethod,
                            fallbackPath = fallbackPath,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (isMissingIcon) {
                        // Use white text on both platforms for visibility on colored backgrounds
                        val firstChar = if (paymentMethod.isNotEmpty()) paymentMethod[0].toString().uppercase() else "?"
                        BisqText.baseBold(
                            text = firstChar,
                            color = BisqTheme.colors.white,
                        )
                    }
                }
            }
        }
        DynamicImage(
            "drawable/payment/interchangeable_grey.png",
            modifier = Modifier.size(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            baseSidePaymentMethods.forEach { settlementMethod ->
                // Check if the settlement method has a known icon file
                val isMissingIcon = !hasKnownSettlementIcon(settlementMethod)
                val customIndex = if(isMissingIcon)
                    customPaymentIconIndex(settlementMethod, CUSTOM_PAYMENT_ICON_IDS.size)
                else
                    0
                val fallbackPath = if (isMissingIcon) "drawable/payment/fiat/${CUSTOM_PAYMENT_ICON_IDS[customIndex]}.png" else null

                Box(contentAlignment = Alignment.Center) {
                    if (isMissingIcon && isIOSPlatform()) {
                        val bgColor = CUSTOM_PAYMENT_BACKGROUND_COLORS.getOrElse(customIndex) {
                            CUSTOM_PAYMENT_BACKGROUND_COLORS[0]
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(bgColor, RoundedCornerShape(4.dp))
                        )
                    } else {
                        DynamicImage(
                            "drawable/payment/bitcoin/${
                                settlementMethod
                                    .lowercase()
                                    .replace("-", "_")
                            }.png",
                            fallbackPath = fallbackPath,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isMissingIcon) {
                        // Use white text on both platforms for visibility on colored backgrounds
                        val firstChar = if (settlementMethod.isNotEmpty()) settlementMethod[0].toString().uppercase() else "?"
                        BisqText.baseBold(
                            text = firstChar,
                            color = BisqTheme.colors.white,
                        )
                    }
                }
            }
        }
    }
}