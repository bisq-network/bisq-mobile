package network.bisq.mobile.presentation

import network.bisq.mobile.presentation.ui.helpers.TimeProvider

expect fun getPlatformCurrentTimeProvider(): TimeProvider

expect fun moveAppToBackground(view: Any?)

expect fun getScreenWidthDp(): Int

// Returns true on Android devices known to crash with dialog-based ModalBottomSheet when toggling window flags
expect fun isAffectedBottomSheetDevice(): Boolean
