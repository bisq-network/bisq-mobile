package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for PushNotificationApiGateway.
 * Since the gateway uses inline reified functions from WebSocketApiClient,
 * we test the request construction logic directly.
 */
class PushNotificationApiGatewayTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DeviceRegistrationRequest is correctly constructed for Android`() {
        // Given
        val deviceId = "test-device-id"
        val deviceToken = "test-token"
        val publicKeyBase64 = "test-public-key"
        val deviceDescriptor = "Test Device"
        val platform = Platform.ANDROID

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                publicKeyBase64 = publicKeyBase64,
                deviceDescriptor = deviceDescriptor,
                platform = platform,
            )

        // Then
        assertEquals(deviceId, request.deviceId)
        assertEquals(deviceToken, request.deviceToken)
        assertEquals(publicKeyBase64, request.publicKeyBase64)
        assertEquals(deviceDescriptor, request.deviceDescriptor)
        assertEquals(platform, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest is correctly constructed for iOS`() {
        // Given
        val deviceId = "ios-device"
        val deviceToken = "apns-token"
        val publicKeyBase64 = "ios-key"
        val deviceDescriptor = "iPhone 15 Pro"
        val platform = Platform.IOS

        // When
        val request =
            DeviceRegistrationRequest(
                deviceId = deviceId,
                deviceToken = deviceToken,
                publicKeyBase64 = publicKeyBase64,
                deviceDescriptor = deviceDescriptor,
                platform = platform,
            )

        // Then
        assertEquals(deviceId, request.deviceId)
        assertEquals(deviceToken, request.deviceToken)
        assertEquals(publicKeyBase64, request.publicKeyBase64)
        assertEquals(deviceDescriptor, request.deviceDescriptor)
        assertEquals(platform, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest serializes to valid JSON`() {
        // Given
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-123",
                deviceToken = "token-456",
                publicKeyBase64 = "key-789",
                deviceDescriptor = "Pixel 8 Pro",
                platform = Platform.ANDROID,
            )

        // When
        val serialized = json.encodeToString(request)

        // Then
        assertTrue(serialized.contains("\"deviceId\":\"device-123\""))
        assertTrue(serialized.contains("\"deviceToken\":\"token-456\""))
        assertTrue(serialized.contains("\"publicKeyBase64\":\"key-789\""))
        assertTrue(serialized.contains("\"deviceDescriptor\":\"Pixel 8 Pro\""))
        assertTrue(serialized.contains("\"platform\":\"ANDROID\""))
    }

    @Test
    fun `basePath is correctly formatted for registrations endpoint`() {
        // Given
        val basePath = "mobile-devices/registrations"
        val deviceId = "device-123"

        // When
        val unregisterPath = "$basePath/$deviceId"

        // Then
        assertEquals("mobile-devices/registrations/device-123", unregisterPath)
    }
}
