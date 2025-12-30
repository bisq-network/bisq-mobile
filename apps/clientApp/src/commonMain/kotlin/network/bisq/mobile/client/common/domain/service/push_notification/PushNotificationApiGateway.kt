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
    private val basePath = "push-notifications"

    /**
     * Register a device for push notifications.
     * @param deviceToken The APNs/FCM device token
     * @param platform The platform (iOS or Android)
     * @return Result indicating success or failure
     */
    suspend fun registerDevice(
        deviceToken: String,
        platform: Platform,
    ): Result<Unit> {
        val request = DeviceRegistrationRequest(deviceToken, platform)
        return webSocketApiClient.post("$basePath/register", request)
    }

    /**
     * Unregister a device from push notifications.
     * @param deviceToken The APNs/FCM device token to unregister
     * @return Result indicating success or failure
     */
    suspend fun unregisterDevice(deviceToken: String): Result<Unit> = webSocketApiClient.delete("$basePath/unregister/$deviceToken")

    /**
     * Check if the current device is registered for push notifications.
     * @return Result containing registration status
     */
    suspend fun isDeviceRegistered(): Result<Boolean> = webSocketApiClient.get("$basePath/status")
}
