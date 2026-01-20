package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.serialization.Serializable

/**
 * Request to register a device for push notifications with the trusted node.
 * The trusted node will store this mapping and use it to send notifications via the relay server.
 */
@Serializable
data class DeviceRegistrationRequest(
    val userProfileId: String,
    val deviceToken: String,
    val publicKey: String,
    val platform: Platform,
)

/**
 * Request to unregister a device from push notifications.
 */
@Serializable
data class UnregisterDeviceRequest(
    val userProfileId: String,
    val deviceToken: String,
)

@Serializable
enum class Platform {
    IOS,
    ANDROID,
}
