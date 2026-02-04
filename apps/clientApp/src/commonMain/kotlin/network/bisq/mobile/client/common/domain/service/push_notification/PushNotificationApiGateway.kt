package network.bisq.mobile.client.common.domain.service.push_notification

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * API Gateway for push notification device registration with the trusted node.
 * The trusted node stores the device token mapping and uses it to send notifications
 * through the relay server when trade events occur.
 *
 * - POST /mobile-devices/registrations - Register device
 * - DELETE /mobile-devices/registrations/{deviceId} - Unregister device
 *
 * TODO: Coverage exclusion rationale - WebSocketApiClient uses inline reified functions
 * (post<T, R>, delete<T>) which cannot be mocked in unit tests. Integration tests with
 * a real or fake HTTP server would be needed for proper coverage.
 */
@ExcludeFromCoverage
class PushNotificationApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "mobile-devices/registrations"

    /**
     * Register a device for push notifications.
     * @param deviceId Unique device identifier (hash of publicKeyBase64 or persisted UUID)
     * @param deviceToken The APNs/FCM device token
     * @param publicKeyBase64 The public key for encrypting notifications (base64 encoded)
     * @param deviceDescriptor Device information (e.g., "iPhone 15 Pro, iOS 17.2")
     * @param platform The platform (iOS or Android)
     * @return Result indicating success or failure
     */
    suspend fun registerDevice(
        deviceId: String,
        deviceToken: String,
        publicKeyBase64: String,
        deviceDescriptor: String,
        platform: Platform,
    ): Result<Unit> {
        val request = DeviceRegistrationRequest(deviceId, deviceToken, publicKeyBase64, deviceDescriptor, platform)
        return webSocketApiClient.post(basePath, request)
    }

    /**
     * Unregister a device from push notifications.
     * @param deviceId The device ID to unregister
     * @return Result indicating success or failure
     */
    suspend fun unregisterDevice(deviceId: String): Result<Unit> = webSocketApiClient.delete("$basePath/$deviceId")
}
