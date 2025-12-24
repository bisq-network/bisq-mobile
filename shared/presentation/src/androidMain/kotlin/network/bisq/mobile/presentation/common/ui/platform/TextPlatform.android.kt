package network.bisq.mobile.presentation.common.ui.platform

import androidx.compose.ui.text.PlatformTextStyle

actual fun platformTextStyleNoFontPadding(): PlatformTextStyle? = PlatformTextStyle(includeFontPadding = false)

actual fun isIOSPlatform(): Boolean = false
