package network.bisq.mobile.presentation.ui.platform

import androidx.compose.ui.text.PlatformTextStyle

actual fun platformTextStyleNoFontPadding(): PlatformTextStyle? =
    PlatformTextStyle(includeFontPadding = false)

