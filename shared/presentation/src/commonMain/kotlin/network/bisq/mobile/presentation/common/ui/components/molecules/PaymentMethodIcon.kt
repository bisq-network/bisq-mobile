package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.platform.CUSTOM_PAYMENT_BACKGROUND_COLORS
import network.bisq.mobile.presentation.common.ui.platform.customPaymentOverlayLetterColor
import network.bisq.mobile.presentation.common.ui.platform.isIOSPlatform
import network.bisq.mobile.presentation.common.ui.platform.platformTextStyleNoFontPadding
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.customPaymentIconIndex
import network.bisq.mobile.presentation.common.ui.utils.hasKnownPaymentIcon
import network.bisq.mobile.presentation.common.ui.utils.hasKnownSettlementIcon

/**
 * Custom payment icon IDs for fallback icons when a payment method doesn't have a known icon.
 */
val CUSTOM_PAYMENT_ICON_IDS =
    listOf(
        "custom_payment_1",
        "custom_payment_2",
        "custom_payment_3",
        "custom_payment_4",
        "custom_payment_5",
        "custom_payment_6",
    )

/**
 * A composable that renders a payment or settlement method icon with fallback support.
 *
 * For methods with known icons, displays the icon image.
 * For methods without known icons, displays a colored background with an overlay letter.
 *
 * @param methodId The payment/settlement method ID (will be trimmed internally)
 * @param isPaymentMethod True for fiat payment methods (uses drawable/payment/fiat/),
 *                        False for settlement methods (uses drawable/payment/bitcoin/)
 * @param size The size of the icon
 * @param alpha Optional alpha value for the icon (default 1f)
 * @param cornerRadius Corner radius for the fallback background (default 4.dp)
 * @param contentDescription Optional content description for accessibility
 * @param useStyledText If true, uses BisqText.styledText with custom font sizing for smaller icons.
 *                      If false, uses BisqText.baseBold (simpler, for larger icons).
 * @param iconPathOverride Optional pre-computed icon path. If null, path is computed from methodId.
 */
@Composable
fun PaymentMethodIcon(
    methodId: String,
    isPaymentMethod: Boolean,
    size: Dp,
    alpha: Float = 1f,
    cornerRadius: Dp = 4.dp,
    contentDescription: String? = null,
    useStyledText: Boolean = false,
    iconPathOverride: String? = null,
) {
    val trimmedId = methodId.trim()
    val hasKnownIcon = if (isPaymentMethod) hasKnownPaymentIcon(trimmedId) else hasKnownSettlementIcon(trimmedId)
    val isMissingIcon = !hasKnownIcon

    val customIndex = if (isMissingIcon) customPaymentIconIndex(trimmedId, CUSTOM_PAYMENT_ICON_IDS.size) else 0
    val overlayLetter = if (isMissingIcon) (trimmedId.firstOrNull()?.uppercase() ?: "?") else null
    val fallbackPath = if (isMissingIcon) "drawable/payment/fiat/${CUSTOM_PAYMENT_ICON_IDS[customIndex]}.png" else null

    val basePath = if (isPaymentMethod) "drawable/payment/fiat" else "drawable/payment/bitcoin"
    val iconPath = iconPathOverride ?: "$basePath/${trimmedId.lowercase().replace("-", "_")}.png"

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (isMissingIcon && isIOSPlatform()) {
            // For custom icons on iOS, use a programmatic colored background
            val bgColor =
                CUSTOM_PAYMENT_BACKGROUND_COLORS.getOrElse(customIndex) {
                    CUSTOM_PAYMENT_BACKGROUND_COLORS[0]
                }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(alpha)
                        .background(bgColor, RoundedCornerShape(cornerRadius)),
            )
        } else {
            // For Android or methods with known icons, use the image
            val imageModifier = Modifier.fillMaxSize().alpha(alpha)
            DynamicImage(
                path = iconPath,
                fallbackPath = fallbackPath,
                contentDescription = contentDescription ?: trimmedId,
                modifier = imageModifier,
            )
        }

        if (isMissingIcon && overlayLetter != null) {
            val letterColor =
                customPaymentOverlayLetterColor(
                    darkColor = BisqTheme.colors.dark_grey20,
                    lightColor = BisqTheme.colors.white,
                )

            if (useStyledText) {
                val letterSizeSp = if (size < 16.dp) 11f else 12f
                BisqText.StyledText(
                    text = overlayLetter,
                    style =
                        BisqTheme.typography.baseBold.copy(
                            fontSize = TextUnit(letterSizeSp, TextUnitType.Sp),
                            lineHeight = TextUnit(letterSizeSp, TextUnitType.Sp),
                            platformStyle = platformTextStyleNoFontPadding(),
                        ),
                    textAlign = TextAlign.Center,
                    color = letterColor,
                )
            } else {
                BisqText.BaseBold(
                    text = overlayLetter,
                    color = letterColor,
                )
            }
        }
    }
}
