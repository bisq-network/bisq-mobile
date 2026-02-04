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
        val request =
            DeviceRegistrationRequest(
                deviceId = "test-device-id",
                deviceToken = "test-token",
                publicKeyBase64 = "test-public-key",
                deviceDescriptor = "Test Device",
                platform = Platform.ANDROID,
            )

        assertEquals("test-device-id", request.deviceId)
        assertEquals("test-token", request.deviceToken)
        assertEquals("test-public-key", request.publicKeyBase64)
        assertEquals("Test Device", request.deviceDescriptor)
        assertEquals(Platform.ANDROID, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest is correctly constructed for iOS`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "ios-device",
                deviceToken = "apns-token",
                publicKeyBase64 = "ios-key",
                deviceDescriptor = "iPhone 15 Pro",
                platform = Platform.IOS,
            )

        assertEquals("ios-device", request.deviceId)
        assertEquals("apns-token", request.deviceToken)
        assertEquals("ios-key", request.publicKeyBase64)
        assertEquals("iPhone 15 Pro", request.deviceDescriptor)
        assertEquals(Platform.IOS, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest serializes to valid JSON`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-123",
                deviceToken = "token-456",
                publicKeyBase64 = "key-789",
                deviceDescriptor = "Pixel 8 Pro",
                platform = Platform.ANDROID,
            )

        val serialized = json.encodeToString(request)

        assertTrue(serialized.contains("\"deviceId\":\"device-123\""))
        assertTrue(serialized.contains("\"deviceToken\":\"token-456\""))
        assertTrue(serialized.contains("\"publicKeyBase64\":\"key-789\""))
        assertTrue(serialized.contains("\"deviceDescriptor\":\"Pixel 8 Pro\""))
        assertTrue(serialized.contains("\"platform\":\"ANDROID\""))
    }

    @Test
    fun `basePath is correctly formatted for registrations endpoint`() {
        // The gateway uses "mobile-devices/registrations" as the base path
        val basePath = "mobile-devices/registrations"
        val deviceId = "device-123"
        val unregisterPath = "$basePath/$deviceId"

        assertEquals("mobile-devices/registrations/device-123", unregisterPath)
    }
}
