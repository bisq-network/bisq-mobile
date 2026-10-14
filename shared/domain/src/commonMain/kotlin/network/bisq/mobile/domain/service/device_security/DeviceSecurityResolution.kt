package network.bisq.mobile.domain.service.device_security

import network.bisq.mobile.data.model.DeviceSecurityBaseline

enum class DeviceSecurityWording {
    FIRST_DETECTION,

    /** The device was clean on the first arrival and is rooted now. */
    CHANGED_SINCE_SETUP,
}

/**
 * Outcome of one dashboard arrival. [baseline] and [suppressed] are the values to persist.
 * [wording] is null when the device is clean.
 */
data class DeviceSecurityResolution(
    val baseline: DeviceSecurityBaseline,
    val suppressed: Boolean,
    val isRooted: Boolean,
    val isPending: Boolean,
    val wording: DeviceSecurityWording?,
)

/**
 * The baseline is recorded on the first arrival and never changed. A clean result clears the
 * suppression: "Don't show again" covers a condition that persists, and a new rooting after a clean
 * observation is new information the earlier decision did not cover.
 */
fun resolveDeviceSecurity(
    integrity: DeviceIntegrity,
    baseline: DeviceSecurityBaseline,
    suppressed: Boolean,
): DeviceSecurityResolution {
    val isRooted = integrity == DeviceIntegrity.ROOTED
    val newBaseline =
        when {
            baseline != DeviceSecurityBaseline.UNSET -> baseline
            isRooted -> DeviceSecurityBaseline.ROOTED
            else -> DeviceSecurityBaseline.CLEAN
        }
    if (!isRooted) {
        return DeviceSecurityResolution(
            baseline = newBaseline,
            suppressed = false,
            isRooted = false,
            isPending = false,
            wording = null,
        )
    }
    return DeviceSecurityResolution(
        baseline = newBaseline,
        suppressed = suppressed,
        isRooted = true,
        isPending = !suppressed,
        wording =
            if (newBaseline == DeviceSecurityBaseline.CLEAN) {
                DeviceSecurityWording.CHANGED_SINCE_SETUP
            } else {
                DeviceSecurityWording.FIRST_DETECTION
            },
    )
}
