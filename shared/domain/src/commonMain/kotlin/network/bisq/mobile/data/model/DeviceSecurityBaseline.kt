package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Device integrity seen on the first dashboard arrival. Recorded once and never overwritten, so a
 * later rooting of a device that was clean at setup can be announced as a change.
 */
@Serializable
enum class DeviceSecurityBaseline {
    UNSET,
    CLEAN,
    ROOTED,
}
