package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.serialization.Serializable

/**
 * Request to register a device for push notifications with the trusted node.
 * The trusted node will store this mapping and use it to send notifications via the relay server.
 *
 * - deviceId: Primary identifier (hash of publicKeyBase64 or persisted UUID)
 * - deviceToken: APNs/FCM device token
 * - publicKeyBase64: Base64-encoded public key for encrypting notifications
 * - deviceDescriptor: Device information (e.g., "iPhone 15 Pro, iOS 17.2")
 * - platform: IOS or ANDROID
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val deviceToken: String,
    val publicKeyBase64: String,
    val deviceDescriptor: String,
    val platform: Platform,
)

@Serializable
enum class Platform {
    IOS,
    ANDROID,
}
