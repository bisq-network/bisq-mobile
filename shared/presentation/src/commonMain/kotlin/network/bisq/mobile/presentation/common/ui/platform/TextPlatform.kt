package network.bisq.mobile.presentation.common.ui.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle

// Expect/actual helper to provide a PlatformTextStyle that disables font padding on platforms that support it.
// On platforms that don't support this flag (e.g., iOS), return null to keep defaults.
expect fun platformTextStyleNoFontPadding(): PlatformTextStyle?

// Returns true if running on iOS platform
expect fun isIOSPlatform(): Boolean

// Returns the appropriate color for custom payment icon overlay letters.
// iOS needs a lighter color due to different text rendering that makes dark text barely visible.
fun customPaymentOverlayLetterColor(
    darkColor: Color,
    lightColor: Color,
): Color = if (isIOSPlatform()) lightColor else darkColor

// Colors for custom payment icon backgrounds (matching Bisq2 desktop custom-payment-*.png)
// These are used on iOS where the PNG images don't render correctly
val CUSTOM_PAYMENT_BACKGROUND_COLORS =
    listOf(
        Color(0xFF5A8A5A), // custom_payment_1 - green
        Color(0xFF8A5A5A), // custom_payment_2 - red/brown
        Color(0xFF5A5A8A), // custom_payment_3 - blue/purple
        Color(0xFF8A8A5A), // custom_payment_4 - olive/yellow
        Color(0xFF5A8A8A), // custom_payment_5 - teal/cyan
        Color(0xFF8A5A8A), // custom_payment_6 - magenta/purple
    )
