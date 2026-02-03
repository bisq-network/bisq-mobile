package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.domain.PlatformType

/**
 * Maps between the domain PlatformType and the push notification Platform enum.
 * This allows dynamic platform detection instead of hardcoding Platform.IOS.
 */
object PlatformMapper {
    /**
     * Convert a PlatformType to the corresponding Platform enum for push notifications.
     * @param platformType The platform type from getPlatformInfo()
     * @return The corresponding Platform enum value
     */
    fun fromPlatformType(platformType: PlatformType): Platform =
        when (platformType) {
            PlatformType.IOS -> Platform.IOS
            PlatformType.ANDROID -> Platform.ANDROID
        }
}
