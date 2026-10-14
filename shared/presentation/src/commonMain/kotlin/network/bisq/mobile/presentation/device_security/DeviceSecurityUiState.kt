package network.bisq.mobile.presentation.device_security

import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.service.device_security.DeviceSecurityWording

/** Which app renders the warning. Selects the body text; provided by each app's DI module. */
enum class DeviceSecurityAppVariant { NODE, CONNECT }

/** Where the sheet was opened from. */
enum class DeviceSecurityOrigin { DASHBOARD, SETTINGS }

/**
 * @property isResolved false until the dashboard arrival has been checked. The dashboard holds the
 * welcome carousel back until then, so the carousel never flashes before the warning.
 * @property isSheetVisible the sheet is on screen: pending on the dashboard and not dismissed in
 * this visit, or opened from the Settings row.
 * @property isRowVisible the Settings row exists only while the device is detected as rooted.
 * @property isSuppressed the persisted "Don't show again" decision.
 */
data class DeviceSecurityUiState(
    val isResolved: Boolean = false,
    val isSheetVisible: Boolean = false,
    val isRowVisible: Boolean = false,
    val isSuppressed: Boolean = false,
    val origin: DeviceSecurityOrigin = DeviceSecurityOrigin.DASHBOARD,
    val wording: DeviceSecurityWording = DeviceSecurityWording.FIRST_DETECTION,
    val app: DeviceSecurityAppVariant = DeviceSecurityAppVariant.CONNECT,
    val platform: PlatformType = PlatformType.ANDROID,
)
