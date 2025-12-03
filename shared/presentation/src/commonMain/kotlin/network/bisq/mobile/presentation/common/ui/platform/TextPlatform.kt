package network.bisq.mobile.presentation.common.ui.platform

import androidx.compose.ui.text.PlatformTextStyle

// Expect/actual helper to provide a PlatformTextStyle that disables font padding on platforms that support it.
// On platforms that don't support this flag (e.g., iOS), return null to keep defaults.
expect fun platformTextStyleNoFontPadding(): PlatformTextStyle?

