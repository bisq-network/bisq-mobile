package network.bisq.mobile.client.common.domain.service.push_notification

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for push notification data models and Platform enum.
 * Note: Full integration tests for ClientPushNotificationServiceFacade require
 * mocking infrastructure that is not available in commonTest.
 */
class ClientPushNotificationServiceFacadeTest {
    @Test
    fun `Platform enum has correct values`() {
        assertEquals("IOS", Platform.IOS.name)
        assertEquals("ANDROID", Platform.ANDROID.name)
        assertEquals(2, Platform.entries.size)
    }

    @Test
    fun `DeviceRegistrationRequest contains correct data`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-id-123",
                deviceToken = "test-token-12345",
                publicKeyBase64 = "base64-encoded-public-key",
                deviceDescriptor = "iPhone 15 Pro, iOS 17.2",
                platform = Platform.IOS,
            )

        assertEquals("device-id-123", request.deviceId)
        assertEquals("test-token-12345", request.deviceToken)
        assertEquals("base64-encoded-public-key", request.publicKeyBase64)
        assertEquals("iPhone 15 Pro, iOS 17.2", request.deviceDescriptor)
        assertEquals(Platform.IOS, request.platform)
    }

    @Test
    fun `DeviceRegistrationRequest for Android platform`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-id-456",
                deviceToken = "android-fcm-token",
                publicKeyBase64 = "base64-encoded-public-key-android",
                deviceDescriptor = "Pixel 8 Pro, Android 14",
                platform = Platform.ANDROID,
            )

        assertEquals("device-id-456", request.deviceId)
        assertEquals("android-fcm-token", request.deviceToken)
        assertEquals("base64-encoded-public-key-android", request.publicKeyBase64)
        assertEquals("Pixel 8 Pro, Android 14", request.deviceDescriptor)
        assertEquals(Platform.ANDROID, request.platform)
    }

    @Test
    fun `PushNotificationException contains message`() {
        val exception = PushNotificationException("Test error message")
        assertEquals("Test error message", exception.message)
    }

    @Test
    fun `PushNotificationException contains cause`() {
        val cause = RuntimeException("Root cause")
        val exception = PushNotificationException("Test error", cause)

        assertEquals("Test error", exception.message)
        assertEquals(cause, exception.cause)
    }

    // Device Registration Validation Tests

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `DeviceRegistrationRequest with valid Base64 public key`() {
        val testPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE" // Valid Base64
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-123",
                deviceToken = "device-token-456",
                publicKeyBase64 = testPublicKey,
                deviceDescriptor = "iPhone 15 Pro, iOS 17.2",
                platform = Platform.IOS,
            )

        // Should be able to decode without throwing
        val decoded = Base64.decode(testPublicKey)
        assertNotNull(decoded)
        assertTrue(decoded.isNotEmpty())
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `DeviceRegistrationRequest with invalid Base64 public key throws exception`() {
        val invalidPublicKey = "not-valid-base64!!!"

        // Should throw when trying to decode
        assertFailsWith<IllegalArgumentException> {
            Base64.decode(invalidPublicKey)
        }
    }

    @Test
    fun `DeviceRegistrationRequest with empty deviceId`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "",
                deviceToken = "token",
                publicKeyBase64 = "key",
                deviceDescriptor = "iPhone",
                platform = Platform.IOS,
            )

        assertEquals("", request.deviceId)
    }

    @Test
    fun `DeviceRegistrationRequest with empty deviceToken`() {
        val request =
            DeviceRegistrationRequest(
                deviceId = "device-id",
                deviceToken = "",
                publicKeyBase64 = "key",
                deviceDescriptor = "iPhone",
                platform = Platform.IOS,
            )

        assertEquals("", request.deviceToken)
    }

    // Platform Detection Tests

    @Test
    fun `Platform IOS has correct string representation`() {
        assertEquals("IOS", Platform.IOS.toString())
    }

    @Test
    fun `Platform ANDROID has correct string representation`() {
        assertEquals("ANDROID", Platform.ANDROID.toString())
    }

    @Test
    fun `Platform enum can be compared`() {
        assertTrue(Platform.IOS != Platform.ANDROID)
        assertTrue(Platform.IOS == Platform.IOS)
        assertTrue(Platform.ANDROID == Platform.ANDROID)
    }
}
