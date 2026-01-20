package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

/**
 * API Gateway for push notification device registration with the trusted node.
 * The trusted node stores the device token mapping and uses it to send notifications
 * through the relay server when trade events occur.
 */
class PushNotificationApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "devices"

    /**
     * Register a device for push notifications.
     * @param userProfileId The user's profile ID
     * @param deviceToken The APNs/FCM device token
     * @param publicKey The public key for encrypting notifications (base64 encoded)
     * @param platform The platform (iOS or Android)
     * @return Result indicating success or failure
     */
    suspend fun registerDevice(
        userProfileId: String,
        deviceToken: String,
        publicKey: String,
        platform: Platform,
    ): Result<Unit> {
        val request = DeviceRegistrationRequest(userProfileId, deviceToken, publicKey, platform)
        return webSocketApiClient.post("$basePath/register", request)
    }

    /**
     * Unregister a device from push notifications.
     * @param userProfileId The user's profile ID
     * @param deviceToken The APNs/FCM device token to unregister
     * @return Result indicating success or failure
     */
    suspend fun unregisterDevice(
        userProfileId: String,
        deviceToken: String,
    ): Result<Unit> {
        val request = UnregisterDeviceRequest(userProfileId, deviceToken)
        return webSocketApiClient.post("$basePath/unregister", request)
    }

    /**
     * Check if the current device is registered for push notifications.
     * @return Result containing registration status
     */
    suspend fun isDeviceRegistered(): Result<Boolean> = webSocketApiClient.get("$basePath/status")
}
