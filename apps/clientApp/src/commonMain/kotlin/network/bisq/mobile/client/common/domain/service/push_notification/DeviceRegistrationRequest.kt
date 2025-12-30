package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.serialization.Serializable

/**
 * Request to register a device for push notifications with the trusted node.
 * The trusted node will store this mapping and use it to send notifications via the relay server.
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceToken: String,
    val platform: Platform,
)

@Serializable
enum class Platform {
    IOS,
    ANDROID,
}
